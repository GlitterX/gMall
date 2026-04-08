create table if not exists settlement_finance_service_skeleton (
    id varchar(64) primary key,
    created_at timestamp not null default current_timestamp
);
