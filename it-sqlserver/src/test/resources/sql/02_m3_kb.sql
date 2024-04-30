create table Concept
(
    AphiaId            bigint,
    LAST_UPDATED_TIME  datetime2(6),
    ParentConceptID_FK bigint,
    id                 bigint not null,
    StructureType      varchar(10),
    RankLevel          varchar(20),
    RankName           varchar(20),
    TaxonomyType       varchar(20),
    Reference          varchar(1024),
    Originator         varchar(255),
    primary key (id)
);
create table ConceptDelegate
(
    ConceptID_FK      bigint not null,
    LAST_UPDATED_TIME datetime2(6),
    id                bigint not null,
    primary key (id)
);
create table ConceptName
(
    ConceptID_FK      bigint       not null,
    LAST_UPDATED_TIME datetime2(6),
    id                bigint       not null,
    NameType          varchar(10)  not null,
    ConceptName       varchar(128) not null,
    Author            varchar(255),
    primary key (id)
);
create table History
(
    Approved             smallint,
    ConceptDelegateID_FK bigint      not null,
    CreationDTG          datetime2(6) not null,
    LAST_UPDATED_TIME    datetime2(6),
    ProcessedDTG         datetime2(6),
    id                   bigint      not null,
    Action               varchar(16),
    CreatorName          varchar(50) not null,
    ProcessorName        varchar(50),
    Field                varchar(2048),
    NewValue             varchar(2048),
    OldValue             varchar(2048),
    primary key (id)
);
create table LinkRealization
(
    ConceptDelegateID_FK bigint not null,
    LAST_UPDATED_TIME    datetime2(6),
    id                   bigint not null,
    LinkName             varchar(50),
    ToConcept            varchar(128),
    LinkValue            varchar(2048),
    primary key (id)
);
create table LinkTemplate
(
    ConceptDelegateID_FK bigint not null,
    LAST_UPDATED_TIME    datetime2(6),
    id                   bigint not null,
    LinkName             varchar(50),
    ToConcept            varchar(128),
    LinkValue            varchar(2048),
    primary key (id)
);
create table Media
(
    PrimaryMedia         bit,
    MediaType            varchar(5),
    ConceptDelegateID_FK bigint not null,
    LAST_UPDATED_TIME    datetime2(6),
    id                   bigint not null,
    Caption              varchar(1000),
    Url                  varchar(1024),
    Credit               varchar(255),
    primary key (id)
);
create table Prefs
(
    PrefKey   varchar(256)  not null,
    PrefValue varchar(256)  not null,
    NodeName  varchar(1024) not null,
    primary key (NodeName, PrefKey)
);
create table UniqueID
(
    NextID    bigint,
    TableName varchar(255) not null,
    primary key (TableName)
);
insert into UniqueID(TableName, NextID)
values ('UserName', 0);
insert into UniqueID(TableName, NextID)
values ('Media', 0);
insert into UniqueID(TableName, NextID)
values ('LinkTemplate', 0);
insert into UniqueID(TableName, NextID)
values ('Concept', 0);
insert into UniqueID(TableName, NextID)
values ('LinkRealization', 0);
insert into UniqueID(TableName, NextID)
values ('History', 0);
insert into UniqueID(TableName, NextID)
values ('ConceptName', 0);
insert into UniqueID(TableName, NextID)
values ('ConceptDelegate', 0);
create table UserAccount
(
    LAST_UPDATED_TIME datetime2(6),
    id                bigint      not null,
    Role              varchar(10) not null,
    Email             varchar(50),
    FirstName         varchar(50),
    LastName          varchar(50),
    Password          varchar(50) not null,
    UserName          varchar(50) not null,
    Affiliation       varchar(512),
    primary key (id)
);
create index idx_Concept_FK1 on Concept (ParentConceptID_FK);
create index idx_Concept_LUT on Concept (LAST_UPDATED_TIME);
create index idx_ConceptDelegate_FK1 on ConceptDelegate (ConceptID_FK);
create index idx_ConceptDelegate_LUT on ConceptDelegate (LAST_UPDATED_TIME);
alter table ConceptDelegate
    add constraint UK2tdrseiaosnbrvl82xvla1qbh unique (ConceptID_FK);
create index idx_ConceptName_name on ConceptName (ConceptName);
create index idx_ConceptName_FK1 on ConceptName (ConceptID_FK);
create index idx_ConceptName_LUT on ConceptName (LAST_UPDATED_TIME);
alter table ConceptName
    add constraint UKrkh258hftvi9qs6b07xckxgl8 unique (ConceptName);
create index idx_History_FK1 on History (ConceptDelegateID_FK);
create index idx_History_LUT on History (LAST_UPDATED_TIME);
create index idx_LinkRealization_FK1 on LinkRealization (ConceptDelegateID_FK);
create index idx_LinkRealization_LUT on LinkRealization (LAST_UPDATED_TIME);
create index idx_LinkTemplate_FK1 on LinkTemplate (ConceptDelegateID_FK);
create index idx_LinkTemplate_LUT on LinkTemplate (LAST_UPDATED_TIME);
create index idx_Media_FK1 on Media (ConceptDelegateID_FK);
create index idx_Media_LUT on Media (LAST_UPDATED_TIME);
alter table UserAccount
    add constraint UKfymnce9g69g5npidfaoevoje4 unique (UserName);
alter table Concept
    add constraint fk_concept__concept_id foreign key (ParentConceptID_FK) references Concept;
alter table ConceptDelegate
    add constraint fk_ConceptDelegage__Concept_id foreign key (ConceptID_FK) references Concept;
alter table ConceptName
    add constraint fk_ConceptName__Concept_id foreign key (ConceptID_FK) references Concept;
alter table History
    add constraint fk_History__ConceptDelegate_id foreign key (ConceptDelegateID_FK) references ConceptDelegate;
alter table LinkRealization
    add constraint fk_LinkRealization__ConceptDelegate_id foreign key (ConceptDelegateID_FK) references ConceptDelegate;
alter table LinkTemplate
    add constraint fk_LinkTempate__ConceptDelegate_id foreign key (ConceptDelegateID_FK) references ConceptDelegate;
alter table Media
    add constraint fk_Media__ConceptDelegate_id foreign key (ConceptDelegateID_FK) references ConceptDelegate;
