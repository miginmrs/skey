drop table if exists $values;
create table $values(id varchar(2), val int) engine=myISAM;

drop procedure if exists parse;
delimiter $$
create procedure parse(param text) begin
	declare str text default reverse(mid(reverse(mid(param, 2)),2));
	declare n int default length(str);
	declare i int default 1;
	declare str2 text default '';
	drop table if exists $temp_values;
	create temporary table $temp_values like $values;
	while length(str2)!=n do
		set str2 = substring_index(str,"','",-i);
		insert into $temp_values(id) values(substring_index(str2,"','",1));
		set i = i+1;
	end while;
	set @i = 1<<(i-1);
	update $temp_values set val = @i where @i:=@i>>1;
	select * from $temp_values;
	delete t from $temp_values t inner join $values p on p.id=t.id and p.val=t.val;
	insert into $values select * from $temp_values;
	drop table $temp_values;
end $$
delimiter ;

call parse(Q(ObjectPermissionsList));
call parse(Q(InsertPermissionsList));
call parse(Q(LinkPermissionsList));
drop procedure parse;

drop function if exists $;
create function $(str text) returns int return (select val from $values where id=str);
