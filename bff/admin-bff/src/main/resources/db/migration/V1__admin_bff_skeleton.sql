create table if not exists admin_bff_skeleton (
    id varchar(64) primary key,
    created_at timestamp not null default current_timestamp
);
