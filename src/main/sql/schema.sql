create table MaxUserId
(
    max_id int not null primary key
);

insert into MaxUserId (max_id) values (0);

create table Events
(
    user_id int not null,
    user_event_id int not null,
    primary key (user_id, user_event_id)
);

create table ReleaseSubscriptionEvents
(
    user_id int not null,
    user_event_id int not null,
    end_date timestamp not null,
    primary key (user_id, user_event_id),
    foreign key (user_id, user_event_id) references Events (user_id, user_event_id)
);

create type GateEventType as ENUM ('ENTER', 'EXIT');

create table GateIOEvents
(
    user_id int not null,
    user_event_id int not null,
    gate_event_type GateEventType not null,
    event_timestamp timestamp not null,
    primary key (user_id, user_event_id),
    foreign key (user_id, user_event_id) references Events (user_id, user_event_id)
);
