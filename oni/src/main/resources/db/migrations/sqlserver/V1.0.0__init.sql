-- -- Drop stuff
-- BEGIN TRY
--     alter table Concept drop constraint if exists fk_Concept__Concept_id
-- GO

--     alter table ConceptName drop constraint if exists uc_ConceptName_name
-- GO

--     alter table ConceptDelegate drop constraint if exists fk_ConceptDelegage__Concept_id
-- GO

--     alter table ConceptName drop constraint if exists fk_ConceptName__Concept_id
-- GO

--     alter table History drop constraint if exists fk_History__ConceptDelegate_id
-- GO

--     alter table LinkRealization drop constraint if exists fk_LinkRealization__ConceptDelegate_id
-- GO

--     alter table LinkTemplate drop constraint if exists fk_LinkTempate__ConceptDelegate_id
-- GO

--     alter table Media drop constraint if exists fk_Media__ConceptDelegate_id
-- GO

--     alter table UserAccount drop constraint if exists uc_UserAccount_UserName
-- GO

-- END TRY
-- BEGIN CATCH
--     DECLARE @errorMessage NVARCHAR(MAX)
-- SELECT @errorMessage = ERROR_MESSAGE()
--            PRINT 'Error occurred: ' + @errorMessage
-- GO

-- END CATCH
-- drop table if exists Artifact
-- GO

-- drop table if exists Concept
-- GO

-- drop table if exists ConceptDelegate
-- GO

-- drop table if exists ConceptName
-- GO

-- drop table if exists History
-- GO

-- drop table if exists LinkRealization
-- GO

-- drop table if exists LinkTemplate
-- GO

-- drop table if exists Media
-- GO

-- drop table if exists Prefs
-- GO

-- drop table if exists UniqueID
-- GO

-- drop table if exists UserAccount
-- GO


create table Concept (AphiaId bigint, LAST_UPDATED_TIME datetime2(6), ParentConceptID_FK bigint, id bigint not null, StructureType varchar(10), RankLevel varchar(20), RankName varchar(20), TaxonomyType varchar(20), Reference varchar(1024), Originator varchar(255), primary key (id))
GO

create table ConceptDelegate (ConceptID_FK bigint not null, LAST_UPDATED_TIME datetime2(6), id bigint not null, primary key (id))
GO

create table ConceptName (ConceptID_FK bigint not null, LAST_UPDATED_TIME datetime2(6), id bigint not null, NameType varchar(10) not null, ConceptName varchar(128) not null, Author varchar(255), primary key (id))
GO

create table History (Approved smallint, ConceptDelegateID_FK bigint not null, CreationDTG datetime2(6) not null, LAST_UPDATED_TIME datetime2(6), ProcessedDTG datetime2(6), id bigint not null, Action varchar(16), CreatorName varchar(50) not null, ProcessorName varchar(50), Field varchar(2048), NewValue varchar(2048), OldValue varchar(2048), primary key (id))
GO

create table LinkRealization (ConceptDelegateID_FK bigint not null, LAST_UPDATED_TIME datetime2(6), id bigint not null, LinkName varchar(50), ToConcept varchar(128), LinkValue varchar(2048), primary key (id))
GO

create table LinkTemplate (ConceptDelegateID_FK bigint not null, LAST_UPDATED_TIME datetime2(6), id bigint not null, LinkName varchar(50), ToConcept varchar(128), LinkValue varchar(2048), primary key (id))
GO

create table Media (PrimaryMedia bit, MediaType varchar(5), ConceptDelegateID_FK bigint not null, LAST_UPDATED_TIME datetime2(6), id bigint not null, Caption varchar(1000), Url varchar(1024), Credit varchar(255), primary key (id))
GO

create table Prefs (PrefKey varchar(256) not null, PrefValue varchar(256) not null, NodeName varchar(1024) not null, primary key (NodeName, PrefKey))
GO

create table Reference (LAST_UPDATED_TIME datetime2(6), id bigint not null, citation varchar(2048) not null, doi varchar(2048), primary key (id))
GO

create table Reference_ConceptDelegate (ConceptDelegateID_FK bigint not null, ReferenceID_FK bigint not null, primary key (ConceptDelegateID_FK, ReferenceID_FK))
GO

create table UniqueID (NextID bigint, TableName varchar(255) not null, primary key (TableName))
GO

insert into UniqueID(TableName, NextID) values ('Reference',0)
GO

insert into UniqueID(TableName, NextID) values ('UserName',0)
GO

insert into UniqueID(TableName, NextID) values ('Media',0)
GO

insert into UniqueID(TableName, NextID) values ('LinkTemplate',0)
GO

insert into UniqueID(TableName, NextID) values ('Concept',0)
GO

insert into UniqueID(TableName, NextID) values ('LinkRealization',0)
GO

insert into UniqueID(TableName, NextID) values ('History',0)
GO

insert into UniqueID(TableName, NextID) values ('ConceptName',0)
GO

insert into UniqueID(TableName, NextID) values ('ConceptDelegate',0)
GO

create table UserAccount (LAST_UPDATED_TIME datetime2(6), id bigint not null, Role varchar(10) not null, Email varchar(50), FirstName varchar(50), LastName varchar(50), Password varchar(50) not null, UserName varchar(50) not null, Affiliation varchar(512), primary key (id))
GO

create index idx_Concept_FK1 on Concept (ParentConceptID_FK)
GO

create index idx_Concept_LUT on Concept (LAST_UPDATED_TIME)
GO

create index idx_ConceptDelegate_FK1 on ConceptDelegate (ConceptID_FK)
GO

create index idx_ConceptDelegate_LUT on ConceptDelegate (LAST_UPDATED_TIME)
GO

alter table ConceptDelegate add constraint uc_ConceptDelegate__ConceptID_FK unique (ConceptID_FK)
GO

-- create index idx_ConceptName_name on ConceptName (ConceptName)
GO

create index idx_ConceptName_FK1 on ConceptName (ConceptID_FK)
GO

create index idx_ConceptName_LUT on ConceptName (LAST_UPDATED_TIME)
GO

-- alter table ConceptName add constraint uc_ConceptName__ConceptName unique (ConceptName)
GO

create index idx_History_FK1 on History (ConceptDelegateID_FK)
GO

create index idx_History_LUT on History (LAST_UPDATED_TIME)
GO

create index idx_LinkRealization_FK1 on LinkRealization (ConceptDelegateID_FK)
GO

create index idx_LinkRealization_LUT on LinkRealization (LAST_UPDATED_TIME)
GO

create index idx_LinkTemplate_FK1 on LinkTemplate (ConceptDelegateID_FK)
GO

create index idx_LinkTemplate_LUT on LinkTemplate (LAST_UPDATED_TIME)
GO

create index idx_Media_FK1 on Media (ConceptDelegateID_FK)
GO

create index idx_Media_LUT on Media (LAST_UPDATED_TIME)
GO

create index idx_Reference_citation on Reference (citation)
GO

create index idx_Reference_LUT on Reference (LAST_UPDATED_TIME)
GO

create unique nonclustered index uc_Reference_doi on Reference (doi) where doi is not null
GO

alter table UserAccount add constraint uc_UserAccount__UserName unique (UserName)
GO

alter table Concept add constraint fk_Concept__Concept_id foreign key (ParentConceptID_FK) references Concept
GO

alter table ConceptDelegate add constraint fk_ConceptDelegage__Concept_id foreign key (ConceptID_FK) references Concept
GO

alter table ConceptDelegate add constraint uc_ConceptDelegage__Concept_id unique (ConceptID_FK)
GO

alter table ConceptName add constraint fk_ConceptName__Concept_id foreign key (ConceptID_FK) references Concept
GO

alter table History add constraint fk_History__ConceptDelegate_id foreign key (ConceptDelegateID_FK) references ConceptDelegate
GO

alter table LinkRealization add constraint fk_LinkRealization__ConceptDelegate_id foreign key (ConceptDelegateID_FK) references ConceptDelegate
GO

alter table LinkTemplate add constraint fk_LinkTempate__ConceptDelegate_id foreign key (ConceptDelegateID_FK) references ConceptDelegate
GO

alter table Media add constraint fk_Media__ConceptDelegate_id foreign key (ConceptDelegateID_FK) references ConceptDelegate
GO

alter table Reference_ConceptDelegate add constraint fk_Reference__ConceptDelegate foreign key (ConceptDelegateID_FK) references ConceptDelegate
GO

alter table Reference_ConceptDelegate add constraint fk_ConceptDelegate__Reference foreign key (ReferenceID_FK) references Reference
GO


-- Add case sensitive ConceptName column
ALTER TABLE ConceptName
ALTER COLUMN ConceptName VARCHAR(128)
        COLLATE SQL_Latin1_General_CP1_CS_AS
GO


create index idx_ConceptName__ConceptName
    on ConceptName (ConceptName)
GO


alter table ConceptName
    add constraint uc_ConceptName__ConceptName
        unique (ConceptName)
GO

