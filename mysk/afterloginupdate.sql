		if NEW.active then
			update `session` s inner join `user` u on s.`user`=u.`id` and u.`login.login_id`=OLD.`id` set s.`state`='changed';
		else
			update `session` s inner join `user` u on s.`user`=u.`id` and u.`login.login_id`=OLD.`id` set s.`state`='inactive';
		end if;