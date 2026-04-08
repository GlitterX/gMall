create table if not exists api_gateway_skeleton (
    id varchar(64) primary key,
    created_at timestamp not null default current_timestamp
);
