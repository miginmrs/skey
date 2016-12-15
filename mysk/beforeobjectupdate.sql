		declare usr int default getuser();
		if new.perms is null then
			set new.perms = (select `default_permissions` from `table` where binary `name`=binary @_);
		end if;
		if usr is not null then
			if not test_perm(usr, 'ue', 'ge', 'oe', old.`user.u_owner`, old.`group.g_owner`, old.`perms`, @_) then
				begin
					declare msg text default concat('user has not permission to update ',old.id,'@',@_);
					signal sqlstate '45002' set message_text = msg;
				end;
			end if;
			if old.`user.u_owner`!=new.`user.u_owner` and new.`user.u_owner`!=usr then
				begin
					declare msg text default concat('user has not permission to lose possession of its elements');
					signal sqlstate '45002' set message_text = msg;
				end;
			end if;
			if new.`group.g_owner`!=old.`group.g_owner` and exists(select 1 from mygroups mg where mg.`uid`=usr and mg.`gid`=old.`group.g_owner`) then
				begin
					declare msg text default concat('user has not permission to change the group of ',old.id,'@',@_);
					signal sqlstate '45002' set message_text = msg;
				end;
			end if;
			if ($('ur')|$('gr')|$('or')) & new.perms & ~old.perms and not test_perm(usr, 'ur', 'gr', 'or', old.`user.u_owner`, old.`group.g_owner`, old.perms, @_) then
				begin
					declare msg text default concat('user has not permission to grant read permission on ',old.id,'@',@_);
					signal sqlstate '45002' set message_text = msg;
				end;
			end if;
			if ($('ud')|$('gd')|$('od')) & new.perms & ~old.perms and not test_perm(usr, 'ud', 'gd', 'od', old.`user.u_owner`, old.`group.g_owner`, old.perms, @_) then
				begin
					declare msg text default concat('user has not permission to grant delete permission on ',old.id,'@',@_);
					signal sqlstate '45002' set message_text = msg;
				end;
			end if;
			if ($('ul')|$('gl')|$('ol')) & new.perms & ~old.perms and not test_perm(usr, 'ul', 'gl', 'ol', old.`user.u_owner`, old.`group.g_owner`, old.perms, @_) then
				begin
					declare msg text default concat('user has not permission to grant link permission on ',old.id,'@',@_);
					signal sqlstate '45002' set message_text = msg;
				end;
			end if;
		end if;