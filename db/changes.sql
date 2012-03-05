-- Adding source columns to ais_vessel_target
ALTER TABLE ais_vessel_target CHANGE source source_type VARCHAR(4) NOT NULL DEFAULT 'LIVE';
ALTER TABLE ais_vessel_target ADD source_country CHAR(3) NULL;
ALTER TABLE ais_vessel_target ADD source_region VARCHAR(12) NULL;
ALTER TABLE ais_vessel_target ADD source_bs VARCHAR(12) NULL;
ALTER TABLE ais_vessel_target ADD source_system VARCHAR(12) NULL;
