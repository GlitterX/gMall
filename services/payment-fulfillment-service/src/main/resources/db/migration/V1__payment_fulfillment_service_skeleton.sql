create table if not exists payment_fulfillment_service_skeleton (
    id varchar(64) primary key,
    created_at timestamp not null default current_timestamp
);
