create table if not exists marketing_content_service_skeleton (
    id varchar(64) primary key,
    created_at timestamp not null default current_timestamp
);
