		declare usr int default getuser();
		if usr is not null and not test_perm(usr, 'ud', 'gd', 'od', old.`user.u_owner`, old.`group.g_owner`, old.`perms`, @_) then
			begin
				declare msg text default concat('user has not permission to delete ',old.id,'@',@_);
				signal sqlstate '45002' set message_text = msg;
			end;
		end if;