create view v_mygroups as
	select 
	if( u.id = t.`user.u_owner`,
		t.global_permissions & 0x00f,
		if( mg.`id` is not null,
			t.global_permissions & 0x0f0,
			t.global_permissions & 0xf00
		)
	) perm, u.id uid
	from `table` t 
	join (select `getuser`() as id) u
	left join mygroups mg where mg.`uid`=u.id and mg.`gid`=t.`group.g_owner`
	where `name`='!|';
