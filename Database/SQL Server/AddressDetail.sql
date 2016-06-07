
CREATE TABLE  IF NOT EXISTS AddressDetails(
AddressId INT NOT NULL AUTO_INCREMENT,
AddressLine1 NVARCHAR(200),
AddressLine2 NVARCHAR(200),
AdCity NVARCHAR(200),
AdState NVARCHAR(200),
AdCrd DATETIME,
AdCrdBy NVARCHAR(200),
AdUpd	DATETIME,
AdUpdBy	NVARCHAR(200),
CONSTRAINT PK_AddressDetails PRIMARY KEY(AddressId)
);