-- alter table if exists Concept drop constraint if exists fk_Concept__Concept_id;
-- alter table if exists ConceptDelegate drop constraint if exists fk_ConceptDelegage__Concept_id;
-- alter table if exists ConceptName drop constraint if exists fk_ConceptName__Concept_id;
-- alter table if exists History drop constraint if exists fk_History__ConceptDelegate_id;
-- alter table if exists LinkRealization drop constraint if exists fk_LinkRealization__ConceptDelegate_id;
-- alter table if exists LinkTemplate drop constraint if exists fk_LinkTempate__ConceptDelegate_id;
-- alter table if exists Media drop constraint if exists fk_Media__ConceptDelegate_id;
-- alter table if exists Reference_ConceptDelegate drop constraint if exists FKsifea3n684ed8l2co24qmgn0l;
-- alter table if exists Reference_ConceptDelegate drop constraint if exists FKg6okgoad1u4167wxfsdt7amui;
-- drop table if exists Concept cascade;
-- drop table if exists ConceptDelegate cascade;
-- drop table if exists ConceptName cascade;
-- drop table if exists History cascade;
-- drop table if exists LinkRealization cascade;
-- drop table if exists LinkTemplate cascade;
-- drop table if exists Media cascade;
-- drop table if exists Prefs cascade;
-- drop table if exists Reference cascade;
-- drop table if exists Reference_ConceptDelegate cascade;
-- drop table if exists UniqueID cascade;
-- drop table if exists UserAccount cascade;
create table Concept (
    AphiaId bigint,
    LAST_UPDATED_TIME timestamp(6),
    ParentConceptID_FK bigint,
    id bigint not null,
    RankLevel varchar(20),
    RankName varchar(20),
    primary key (id)
);

create table ConceptDelegate (
    ConceptID_FK bigint not null unique,
    LAST_UPDATED_TIME timestamp(6),
    id bigint not null,
    primary key (id)
);

create table ConceptName (
    ConceptID_FK bigint not null,
    LAST_UPDATED_TIME timestamp(6),
    id bigint not null,
    NameType varchar(10) not null,
    ConceptName varchar(128) not null unique,
    Author varchar(255),
    primary key (id)
);

create table History (
    Approved smallint,
    ConceptDelegateID_FK bigint not null,
    CreationDTG timestamp(6) not null,
    LAST_UPDATED_TIME timestamp(6),
    ProcessedDTG timestamp(6),
    id bigint not null,
    Action varchar(16),
    CreatorName varchar(50) not null,
    ProcessorName varchar(50),
    Field varchar(2048),
    NewValue varchar(2048),
    OldValue varchar(2048),
    primary key (id)
);

create table LinkRealization (
    ConceptDelegateID_FK bigint not null,
    LAST_UPDATED_TIME timestamp(6),
    id bigint not null,
    LinkName varchar(50),
    ToConcept varchar(128),
    LinkValue varchar(2048),
    primary key (id)
);

create table LinkTemplate (
    ConceptDelegateID_FK bigint not null,
    LAST_UPDATED_TIME timestamp(6),
    id bigint not null,
    LinkName varchar(50),
    ToConcept varchar(128),
    LinkValue varchar(2048),
    primary key (id)
);

create table Media (
    PrimaryMedia smallint,
    MediaType varchar(5),
    ConceptDelegateID_FK bigint not null,
    LAST_UPDATED_TIME timestamp(6),
    id bigint not null,
    Caption varchar(1000),
    Url varchar(1024),
    Credit varchar(255),
    primary key (id)
);

create table Prefs (
    PrefKey varchar(256) not null,
    PrefValue varchar(256) not null,
    NodeName varchar(1024) not null,
    primary key (NodeName, PrefKey)
);

create table Reference (
    LAST_UPDATED_TIME timestamp(6),
    id bigint not null,
    citation varchar(2048) not null,
    doi varchar(2048) unique,
    primary key (id)
);

create table Reference_ConceptDelegate (
    ConceptDelegateID_FK bigint not null,
    ReferenceID_FK bigint not null,
    primary key (ConceptDelegateID_FK, ReferenceID_FK)
);

create table UniqueID (
    NextID bigint,
    TableName varchar(255) not null,
    primary key (TableName)
);

insert into
    UniqueID(TableName, NextID)
values
    ('Reference', 0);

insert into
    UniqueID(TableName, NextID)
values
    ('UserName', 0);

insert into
    UniqueID(TableName, NextID)
values
    ('Media', 0);

insert into
    UniqueID(TableName, NextID)
values
    ('LinkTemplate', 0);

insert into
    UniqueID(TableName, NextID)
values
    ('Concept', 0);

insert into
    UniqueID(TableName, NextID)
values
    ('LinkRealization', 0);

insert into
    UniqueID(TableName, NextID)
values
    ('History', 0);

insert into
    UniqueID(TableName, NextID)
values
    ('ConceptName', 0);

insert into
    UniqueID(TableName, NextID)
values
    ('ConceptDelegate', 0);

create table UserAccount (
    LAST_UPDATED_TIME timestamp(6),
    id bigint not null,
    Role varchar(10) not null,
    Email varchar(50),
    FirstName varchar(50),
    LastName varchar(50),
    Password varchar(50) not null,
    UserName varchar(50) not null unique,
    Affiliation varchar(512),
    primary key (id)
);

create index idx_Concept_FK1 on Concept (ParentConceptID_FK);

create index idx_Concept_LUT on Concept (LAST_UPDATED_TIME);

create index idx_ConceptDelegate_FK1 on ConceptDelegate (ConceptID_FK);

create index idx_ConceptDelegate_LUT on ConceptDelegate (LAST_UPDATED_TIME);

create index idx_ConceptName_name on ConceptName (ConceptName);

create index idx_ConceptName_FK1 on ConceptName (ConceptID_FK);

create index idx_ConceptName_LUT on ConceptName (LAST_UPDATED_TIME);

create index idx_History_FK1 on History (ConceptDelegateID_FK);

create index idx_History_LUT on History (LAST_UPDATED_TIME);

create index idx_LinkRealization_FK1 on LinkRealization (ConceptDelegateID_FK);

create index idx_LinkRealization_LUT on LinkRealization (LAST_UPDATED_TIME);

create index idx_LinkTemplate_FK1 on LinkTemplate (ConceptDelegateID_FK);

create index idx_LinkTemplate_LUT on LinkTemplate (LAST_UPDATED_TIME);

create index idx_Media_FK1 on Media (ConceptDelegateID_FK);

create index idx_Media_LUT on Media (LAST_UPDATED_TIME);

-- create index idx_Reference_name on Reference (ReferenceName);
create index idx_Reference_ConceptDelegate_FK1 on Reference_ConceptDelegate (ConceptDelegateID_FK);

create index idx_Reference_LUT on Reference (LAST_UPDATED_TIME);

alter table
    if exists Concept
add
    constraint fk_Concept__Concept_id foreign key (ParentConceptID_FK) references Concept;

alter table
    if exists ConceptDelegate
add
    constraint fk_ConceptDelegage__Concept_id foreign key (ConceptID_FK) references Concept;

alter table
    if exists ConceptName
add
    constraint fk_ConceptName__Concept_id foreign key (ConceptID_FK) references Concept;

alter table
    if exists History
add
    constraint fk_History__ConceptDelegate_id foreign key (ConceptDelegateID_FK) references ConceptDelegate;

alter table
    if exists LinkRealization
add
    constraint fk_LinkRealization__ConceptDelegate_id foreign key (ConceptDelegateID_FK) references ConceptDelegate;

alter table
    if exists LinkTemplate
add
    constraint fk_LinkTempate__ConceptDelegate_id foreign key (ConceptDelegateID_FK) references ConceptDelegate;

alter table
    if exists Media
add
    constraint fk_Media__ConceptDelegate_id foreign key (ConceptDelegateID_FK) references ConceptDelegate;

alter table
    if exists Reference_ConceptDelegate
add
    constraint fk_RCD__ConceptDelegate_id foreign key (ConceptDelegateID_FK) references ConceptDelegate;

alter table
    if exists Reference_ConceptDelegate
add
    constraint fk_RCD__Reference_id foreign key (ReferenceID_FK) references Reference;