		declare usr int default getuser();
		declare grp int;
		if @perms is null or usr is not null then
			set @perms = (select `default_permissions` from `table` where binary `name`=binary @_);
		end if;
		if usr is not null then
			if @user.u_owner is null then
				set @user.u_owner = usr;
			end if;
			set grp = (select `group.group_id` from `user` where id=usr);
			if @group.g_owner is null or exists(select 1 from mygroups mg where mg.`uid`=usr and mg.`gid`=grp) then
				set @group.g_owner = grp;
			end if;
			if !exists(
				select * from `table` t where `name`=@_ and if( usr = t.`user.u_owner`,
					t.insert_permission & $('ui'), 
					if( exists(select * from mygroups mg where mg.`uid`=usr and mg.`gid`=t.`group.g_owner`), 
						t.insert_permission & $('gi'), 
						t.insert_permission & $('oi')
					)
				)
			)
			then 
				begin
					declare msg text default concat('user has not permission to insert into ',@_);
					signal sqlstate '45002' set message_text = msg;
				end;
			end if;
		end if;