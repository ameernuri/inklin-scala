# --- !Ups

create table inkler (
	id                                  bigint not null primary key,
	username                            varchar(15) not null,
	first_name                          varchar(30) not null,
	last_name                           varchar(30) not null,
	email                               varchar(100) not null,
	password                            varchar(100) not null,
	invitation_code                     varchar(255) not null default 0,

	unique (email),
	unique (username)
);

create sequence inkler_seq start with 1000;

create table box (
	id                                  bigint not null primary key,
	created_by                          bigint not null,
	name                                varchar(25) not null,
	secret                              boolean default false,
	max_r                               bigint default 0,

	foreign key(created_by)             references inkler(id) on delete cascade
);

create sequence box_seq start 1000;

create table box_member (
	box_id                  						bigint not null,
	inkler_id                 				  bigint not null,

	foreign key(box_id)     						references box(id) on delete cascade,
	foreign key(inkler_id)    					references inkler(id) on delete cascade
);

create table box_follow (
	box_id                  						bigint not null,
	follower_id             						bigint not null,

	foreign key(box_id)     						references box(id) on delete cascade,
	foreign key(follower_id)					  references inkler(id) on delete cascade
);


create table inkle (
	id                      						bigint not null primary key,
	created_by              						bigint not null,
	inkle                    						varchar(70) not null,
	box_id                  						bigint not null,
	parent_id               						bigint default null,
	l                       						bigint not null,
	r                       						bigint not null,
	created                             timestamp not null,

	foreign key(box_id)                 references box(id) on delete cascade,
	foreign key(parent_id)              references inkle(id) on delete cascade,
	foreign key(created_by)             references inkler(id) on delete cascade
);

create sequence inkle_seq start 1000;

create table bump (
	inkle_id                             bigint not null,
	created                              timestamp not null,

	foreign key(inkle_id)                references inkle(id) on delete cascade
);

# --- !Downs

drop table if exists box cascade;
drop sequence if exists box_seq;

drop table if exists box_member;
drop table if exists box_follow;

drop table if exists inkle cascade;
drop sequence if exists inkle_seq;

drop table if exists inkler cascade;
drop sequence if exists inkler_seq;

drop table if exists bump;


