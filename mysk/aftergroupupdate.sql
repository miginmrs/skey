		if NEW.`group.parent` != OLD.`group.parent` then
			update `session` s inner join `user` u on s.`user` = u.id inner join mygroups mg on mg.`uid`=u.id and mg.`gid`=OLD.`group.parent` set s.`state`='updated';
		end if;