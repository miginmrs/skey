drop table if exists link;
create table link(data int) engine=memory as select null data;

drop table if exists default_link;
create table default_link(data int) engine=memory as select null data;

drop table if exists mygroups;
create table mygroups(data int) engine=memory as select null data;

drop function if exists `pstr`;
create function `pstr`(p int) returns varchar( 4) deterministic no sql
	return concat(if(p&1,'r','-'),  if(p&2,'e','-'),  if(p&4,'d','-'),  if(p&8,'l','-'));

drop function if exists `perm`;
create function `perm`(p int) returns varchar(25) deterministic no sql
	return concat(pstr(p),pstr(p>>4),pstr(p>>8),' ',pstr(p>>12),pstr(p>>16),pstr(9>>20));

drop procedure if exists `mgupdate`;
delimiter $$
create procedure `mgupdate`(usr int) reads sql data sql security invoker
begin
	delete from mygroups where `uid`=usr;
	create temporary table if not exists newgroups(`gid` int);
	create temporary table if not exists oldgroups(`gid` int);
	delete from newgroups;
	delete from oldgroups;
	insert into mygroups(`uid`, `gid`) select `user.user_id`, `group.group_id` from `groupmember` gm where gm.`user.user_id` = usr;
	insert into oldgroups select `gid` from mygroups;
	while row_count() > 0 do
		insert into newgroups(`gid`) select g.`id` from oldgroups og inner join `group` g on g.`group.parent`!=g.`id` and g.`group.parent` = og.`gid`;
		delete from oldgroups;
		insert into mygroups(`uid`, `gid`) select usr, `gid` from newgroups;
		insert into oldgroups select * from newgroups;
		delete from newgroups;
	end while;
end $$
delimiter ;

drop function if exists `getuser`;
delimiter $$
create function `getuser`() returns int reads sql data
begin
	declare usr int;
	declare `found` bool default false;
	declare connected bool;
	declare state UserState;
	declare v_session char(26);
	create temporary table if not exists `tempdata`(`user` int, `session` char(26)) engine=memory;
	select `user`,`session` into usr, v_session from `tempdata`;
	if usr is null then 
		return null; 
	end if;
	select s.`state`, binary v_session= binary s.`session`, true into state, connected, `found` from `session` s where usr=`user`;
	if not `found` then
		signal sqlstate '45001' set message_text = 'user deleted';
	end if;
	if not `connected` then
		signal sqlstate '45001' set message_text = 'connection lost';
	end if;
	if state!='active' then
		if state='updated' then
			call mgupdate(usr);
		else 
			begin
				declare msg text default concat('user ', `state`);
				signal sqlstate '45001' set message_text = msg;
			end;
		end if;
	end if;
	return usr;
end $$
delimiter ;
create view v_getuser as select `getuser`() id;

drop function if exists `link_perm`;
delimiter $$
create function `link_perm`(usr int, tab text, ref text, val int) returns int reads sql data begin
	declare perm int;
	if usr is null or val is null or exists (
		select 1 from `object` t where id=val and if( usr = t.`user.u_owner`,
			t.perms & $('ul'), 
			if(exists(select * from mygroups mg where mg.`uid`=usr and mg.`gid`=t.`group.g_owner`), 
				t.perms & $('gl'), 
				t.perms & $('ol')
			)
		)
	) or exists(
		select * from `table` t where `name`=ref and if( usr = t.`user.u_owner`,
			t.global_permissions & $('ul'), 
			if(exists(select * from mygroups mg where mg.`uid`=usr and mg.`gid`=t.`group.g_owner`), 
				t.global_permissions & $('gl'), 
				t.global_permissions & $('ol')
			)
		)
	) then 
		return true; 
	end if;
	set perm = (
		select if(usr = t.`user.u_owner`,
			t.link_permissions & $('uk'), 
			if( exists(select * from mygroups mg where mg.`uid`=usr and mg.`gid`=t.`group.g_owner`), 
				t.link_permissions & $('gk'), 
				t.link_permissions & $('ok')
			)
		) from link t where `object.linked`=val and `table.linker`=tab
	);
	if perm is not null then return perm; end if;
	return exists (
		select 1 from default_link t where `table.linked`=ref and `table.linker`=tab and if( usr = t.`user.u_owner`,
			t.link_permissions & $('uk'), 
			if(exists(select * from mygroups mg where mg.`uid`=usr and mg.`gid`=t.`group.g_owner`), 
				t.link_permissions & $('gk'), 
				t.link_permissions & $('ok')
			)
		)
	);
end $$
delimiter ;

drop function if exists `connect`;
delimiter $$
create function `connect`(p_login text, p_pass text, p_ip text, p_captcha tinyint, p_session char(26)) returns int reads sql data modifies sql data sql security invoker
begin
	declare last timestamp default now() - DELETEDELAY;
	declare usr int;
	declare active boolean;
	delete from ip where timestamp < last;
	if not p_captcha and (select count(*) from ip where ip=p_ip and now()) = 3 then
		return -3;
	end if;
	select user.id, login.active into usr, active from login inner join `user` on `login.login_id`=login.id where login.login = p_login and login.pass = p_pass;
	if usr is null then
		if not p_captcha then 
			insert into ip(ip, timestamp) values(p_ip, now());
		end if;
		if (select count(*) from ip where ip=p_ip and now()) = 3 then 
			return -2;
		end if;
		return -1;
	end if;
	if not active then 
		return 0;
	end if;
	insert into `session`(`user`, `session`, `timestamp`) values (usr, p_session, now()) on duplicate key update `session`=p_session, `timestamp`=now();
	call mgupdate(usr);
	return usr;
end $$
delimiter ;

drop function if exists `auth`;
delimiter $$
create procedure `auth`(usr int, session varchar(26)) contains sql begin
	create temporary table `tempdata`(`user` int, `session` varchar(26)) engine=memory as select usr user, session session;
end $$
delimiter ;

drop function if exists `test_perm`;
delimiter $$
create function `test_perm`(usr int, u text, g text, o text, u_owner int, g_owner int, perms int, tab text) returns int reads sql data
	return if( usr = u_owner, 
		perms & $(u),
		if( exists(select * from mygroups mg where mg.`uid`=usr and mg.`gid`=g_owner),
			perms & $(g),
			perms & $(o)
		)
	) or exists(
		select * from `table` t where `name`=tab and if( usr = t.`user.u_owner`,
			t.global_permissions & $(u), 
			if(exists(select * from mygroups mg where mg.`uid`=usr and mg.`gid`=t.`group.g_owner`), 
				t.global_permissions & $(g), 
				t.global_permissions & $(o)
			)
		)
	)
$$
delimiter ;
