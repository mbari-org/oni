CREATE TABLE "dbo"."Artifact"  ( 
	"ConceptDelegateID_FK"	bigint NOT NULL,
	"GroupId"             	varchar(64) NOT NULL,
	"ArtifactId"          	varchar(256) NOT NULL,
	"Version"             	varchar(64) NOT NULL,
	"Classifier"          	varchar(64) NULL,
	"Description"         	varchar(2048) NULL,
	"MimeType"            	varchar(32) NULL,
	"Caption"             	varchar(1024) NULL,
	"Reference"           	varchar(1024) NOT NULL,
	"Credit"              	varchar(1024) NULL,
	"id"                  	bigint NOT NULL,
	"LAST_UPDATED_TIME"   	datetime NOT NULL,
	"CreationDate"        	datetime NULL
	CONSTRAINT "Artifact_PK" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);
CREATE TABLE "dbo"."Concept"  ( 
	"id"                	bigint NOT NULL,
	"ParentConceptID_FK"	bigint NULL,
	"Originator"        	varchar(255) NULL,
	"StructureType"     	varchar(10) NULL,
	"Reference"         	varchar(1024) NULL,
	"NodcCode"          	varchar(20) NULL,
	"RankName"          	varchar(20) NULL,
	"RankLevel"         	varchar(20) NULL,
	"TaxonomyType"      	varchar(20) NULL,
	"LAST_UPDATED_TIME" 	datetime NULL
	CONSTRAINT "Concept_PK" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);
CREATE TABLE "dbo"."ConceptDelegate"  ( 
	"id"               	bigint NOT NULL,
	"ConceptID_FK"     	bigint NOT NULL,
	"LAST_UPDATED_TIME"	datetime NULL
	CONSTRAINT "ConceptDelegate_PK" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);
CREATE TABLE "dbo"."ConceptName"  ( 
	"ConceptName"      	varchar(128) NULL,
	"ConceptID_FK"     	bigint NULL,
	"Author"           	varchar(255) NULL,
	"NameType"         	varchar(10) NULL,
	"id"               	bigint NOT NULL,
	"LAST_UPDATED_TIME"	datetime NULL
	CONSTRAINT "ConceptName_PK" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);
CREATE TABLE "dbo"."History"  ( 
	"id"                  	bigint NOT NULL,
	"ConceptDelegateID_FK"	bigint NULL,
	"ProcessedDTG"        	datetime NULL,
	"CreationDTG"         	datetime NULL,
	"Description"         	varchar(1000) NULL,
	"CreatorName"         	varchar(50) NULL,
	"ProcessorName"       	varchar(50) NULL,
	"Field"               	varchar(50) NULL,
	"OldValue"            	varchar(2048) NULL,
	"NewValue"            	varchar(2048) NULL,
	"Action"              	varchar(16) NULL,
	"Comment"             	varchar(2048) NULL,
	"Approved"            	smallint NOT NULL,
	"LAST_UPDATED_TIME"   	datetime NULL
	CONSTRAINT "History_PK" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);
CREATE TABLE "dbo"."LinkRealization"  ( 
	"id"                  	bigint NOT NULL,
	"ConceptDelegateID_FK"	bigint NULL,
	"LinkName"            	varchar(50) NULL,
	"ToConcept"           	varchar(128) NULL,
	"LinkValue"           	varchar(2048) NULL,
	"LAST_UPDATED_TIME"   	datetime NULL
	CONSTRAINT "LinkRealization_PK" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);
CREATE TABLE "dbo"."LinkTemplate"  ( 
	"id"                  	bigint NOT NULL,
	"ConceptDelegateID_FK"	bigint NULL,
	"LinkName"            	varchar(50) NULL,
	"ToConcept"           	varchar(128) NULL,
	"LinkValue"           	varchar(2048) NULL,
	"LAST_UPDATED_TIME"   	datetime NULL
	CONSTRAINT "LinkTemplate_PK" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);
CREATE TABLE "dbo"."Media"  ( 
	"id"                  	bigint NOT NULL,
	"ConceptDelegateID_FK"	bigint NULL,
	"Url"                 	varchar(1024) NULL,
	"MediaType"           	char(5) NULL,
	"PrimaryMedia"        	bit NULL,
	"Credit"              	varchar(255) NULL,
	"Caption"             	varchar(1000) NULL,
	"LAST_UPDATED_TIME"   	datetime NULL
	CONSTRAINT "Media_PK" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);
CREATE TABLE "dbo"."Prefs"  ( 
	"NodeName" 	nvarchar(255) NOT NULL,
	"PrefKey"  	nvarchar(50) NOT NULL,
	"PrefValue"	nvarchar(255) NOT NULL
	);
CREATE TABLE "dbo"."SectionInfo"  ( 
	"id"                  	bigint NOT NULL,
	"ConceptDelegateID_FK"	bigint NOT NULL,
	"Header"              	varchar(30) NOT NULL,
	"Label"               	varchar(50) NOT NULL,
	"Information"         	varchar(5000) NULL,
	CONSTRAINT "SectionInfo_PK" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);
CREATE TABLE "dbo"."UniqueID"  ( 
	"tablename"	varchar(200) NOT NULL,
	"nextid"   	bigint NULL,
	CONSTRAINT "UniqueID_PK" PRIMARY KEY CLUSTERED("tablename")
 ON [PRIMARY]);
CREATE TABLE "dbo"."Usage"  ( 
	"id"                   	bigint NOT NULL,
	"ConceptDelegateID_FK" 	bigint NULL,
	"EmbargoExpirationDate"	datetime NULL,
	"Specification"        	varchar(1000) NULL,
	"LAST_UPDATED_TIME"    	datetime NULL
	CONSTRAINT "Usage_PK" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);
CREATE TABLE "dbo"."UserAccount"  ( 
	"id"               	bigint NOT NULL,
	"UserName"         	varchar(50) NOT NULL,
	"Password"         	varchar(50) NULL,
	"Role"             	varchar(10) NULL,
	"LAST_UPDATED_TIME"	datetime NULL,
	"Affiliation"      	varchar(50) NULL,
	"FirstName"        	varchar(50) NULL,
	"LastName"         	varchar(50) NULL,
	"Email"            	varchar(50) NULL
	CONSTRAINT "UserAccount_PK" PRIMARY KEY CLUSTERED("id")
 ON [PRIMARY]);

CREATE UNIQUE NONCLUSTERED INDEX "idx_Artifact_CK"
	ON "dbo"."Artifact"("ConceptDelegateID_FK", "GroupId", "ArtifactId", "Version", "Classifier");
CREATE NONCLUSTERED INDEX "idx_Artifact_FK1"
	ON "dbo"."Artifact"("ConceptDelegateID_FK");
CREATE NONCLUSTERED INDEX "idx_Artifact_LUT"
	ON "dbo"."Artifact"("LAST_UPDATED_TIME");
CREATE NONCLUSTERED INDEX "idx_ConceptDelegate_FK1"
	ON "dbo"."ConceptDelegate"("ConceptID_FK");
CREATE NONCLUSTERED INDEX "idx_ConceptDelegate_LUT"
	ON "dbo"."ConceptDelegate"("LAST_UPDATED_TIME");
CREATE NONCLUSTERED INDEX "idx_ConceptName_FK1"
	ON "dbo"."ConceptName"("ConceptID_FK");
CREATE NONCLUSTERED INDEX "idx_ConceptName_LUT"
	ON "dbo"."ConceptName"("LAST_UPDATED_TIME");
CREATE NONCLUSTERED INDEX "idx_ConceptName_name"
	ON "dbo"."ConceptName"("ConceptName");
CREATE NONCLUSTERED INDEX "idx_Concept_FK1"
	ON "dbo"."Concept"("ParentConceptID_FK");
CREATE NONCLUSTERED INDEX "idx_Concept_LUT"
	ON "dbo"."Concept"("LAST_UPDATED_TIME");
CREATE NONCLUSTERED INDEX "idx_History_FK1"
	ON "dbo"."History"("ConceptDelegateID_FK");
CREATE NONCLUSTERED INDEX "idx_History_LUT"
	ON "dbo"."History"("LAST_UPDATED_TIME");
CREATE NONCLUSTERED INDEX "idx_LinkRealization_FK1"
	ON "dbo"."LinkRealization"("ConceptDelegateID_FK");
CREATE NONCLUSTERED INDEX "idx_LinkRealization_LUT"
	ON "dbo"."LinkRealization"("LAST_UPDATED_TIME");
CREATE NONCLUSTERED INDEX "idx_LinkTemplate_FK1"
	ON "dbo"."LinkTemplate"("ConceptDelegateID_FK");
CREATE NONCLUSTERED INDEX "idx_LinkTemplate_LUT"
	ON "dbo"."LinkTemplate"("LAST_UPDATED_TIME");
CREATE NONCLUSTERED INDEX "idx_Media_LUT"
	ON "dbo"."Media"("LAST_UPDATED_TIME");
CREATE NONCLUSTERED INDEX "idx_Prefs"
	ON "dbo"."Prefs"("NodeName", "PrefKey");
CREATE NONCLUSTERED INDEX "idx_SectionInfo_FK1"
	ON "dbo"."SectionInfo"("ConceptDelegateID_FK");
CREATE NONCLUSTERED INDEX "idx_Usage_FK1"
	ON "dbo"."Usage"("ConceptDelegateID_FK");
CREATE NONCLUSTERED INDEX "idx_Usage_LUT"
	ON "dbo"."Usage"("LAST_UPDATED_TIME");
CREATE NONCLUSTERED INDEX "idx_UserAccount_UserName"
	ON "dbo"."UserAccount"("UserName");

ALTER TABLE "dbo"."ConceptName"
	ADD CONSTRAINT "uc_ConceptName_name"
	UNIQUE ("ConceptName");
ALTER TABLE "dbo"."UserAccount"
	ADD CONSTRAINT "uc_UserAccount_UserName"
	UNIQUE ("UserName");
ALTER TABLE "dbo"."Artifact" WITH NOCHECK
	ADD CONSTRAINT "fk_Artifact__ConceptDelegate_id"
	FOREIGN KEY("ConceptDelegateID_FK")
	REFERENCES "dbo"."ConceptDelegate"("id")
	ON DELETE NO ACTION 
	ON UPDATE NO ACTION 
	NOT FOR REPLICATION ;
ALTER TABLE "dbo"."Concept"
    ADD CONSTRAINT "fk_Concept__Concept_id"
        FOREIGN KEY("ParentConceptID_FK")
            REFERENCES "dbo"."Concept"("id")
            ON DELETE NO ACTION
            ON UPDATE NO ACTION
    NOT FOR REPLICATION ;
ALTER TABLE "dbo"."ConceptDelegate"
	ADD CONSTRAINT "fk_ConceptDelegate__Concept_id"
	FOREIGN KEY("ConceptID_FK")
	REFERENCES "dbo"."Concept"("id")
	ON DELETE NO ACTION 
	ON UPDATE NO ACTION 
	NOT FOR REPLICATION ;
ALTER TABLE "dbo"."ConceptDelegate"
	NOCHECK CONSTRAINT "ConceptDelegate_FK1";
ALTER TABLE "dbo"."ConceptName" WITH NOCHECK
	ADD CONSTRAINT "fk_ConceptName__Concept_id"
	FOREIGN KEY("ConceptID_FK")
	REFERENCES "dbo"."Concept"("id")
	ON DELETE NO ACTION 
	ON UPDATE NO ACTION 
	NOT FOR REPLICATION ;
ALTER TABLE "dbo"."History" WITH NOCHECK
	ADD CONSTRAINT "fk_History__ConceptDelegate_id"
	FOREIGN KEY("ConceptDelegateID_FK")
	REFERENCES "dbo"."ConceptDelegate"("id")
	ON DELETE NO ACTION 
	ON UPDATE NO ACTION 
	NOT FOR REPLICATION ;
ALTER TABLE "dbo"."LinkRealization" WITH NOCHECK
	ADD CONSTRAINT "fk_LinkRealization__ConceptDelegate_id"
	FOREIGN KEY("ConceptDelegateID_FK")
	REFERENCES "dbo"."ConceptDelegate"("id")
	ON DELETE NO ACTION 
	ON UPDATE NO ACTION 
	NOT FOR REPLICATION ;
ALTER TABLE "dbo"."LinkTemplate" WITH NOCHECK
	ADD CONSTRAINT "fk_LinkTemplate__ConceptDelegate_id"
	FOREIGN KEY("ConceptDelegateID_FK")
	REFERENCES "dbo"."ConceptDelegate"("id")
	ON DELETE NO ACTION 
	ON UPDATE NO ACTION 
	NOT FOR REPLICATION ;
ALTER TABLE "dbo"."Media" WITH NOCHECK
	ADD CONSTRAINT "fk_Media__ConceptDelegate_id"
	FOREIGN KEY("ConceptDelegateID_FK")
	REFERENCES "dbo"."ConceptDelegate"("id")
	ON DELETE NO ACTION 
	ON UPDATE NO ACTION 
	NOT FOR REPLICATION ;
ALTER TABLE "dbo"."SectionInfo" WITH NOCHECK
	ADD CONSTRAINT "fk_SectionInfo__ConceptDelegate_id"
	FOREIGN KEY("ConceptDelegateID_FK")
	REFERENCES "dbo"."ConceptDelegate"("id")
	ON DELETE NO ACTION 
	ON UPDATE NO ACTION 
	NOT FOR REPLICATION ;
ALTER TABLE "dbo"."Usage" WITH NOCHECK
	ADD CONSTRAINT "fk_Usage__ConceptDelegate_id"
	FOREIGN KEY("ConceptDelegateID_FK")
	REFERENCES "dbo"."ConceptDelegate"("id")
	ON DELETE NO ACTION 
	ON UPDATE NO ACTION 
	NOT FOR REPLICATION ;
