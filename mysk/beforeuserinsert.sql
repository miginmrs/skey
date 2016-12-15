		if @group.group_id is null then
			set @group.group_id = (select `group.group_id` from `user` where id = getuser());
		end if;