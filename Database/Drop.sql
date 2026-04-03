-- =============================================================
--  CloudVault — Teardown Script
--  File    : Drop.sql
--
--  !! WARNING — DEV/RESET USE ONLY !!
--  !! DO NOT RUN IN PRODUCTION      !!
--
--  Drops all 6 CloudVault tables in FK-safe order.
--  All data will be permanently destroyed.
-- =============================================================

USE cloudvault;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS activity_logs;
DROP TABLE IF EXISTS share_links;
DROP TABLE IF EXISTS file_permissions;
DROP TABLE IF EXISTS files;
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- To also drop the entire database, uncomment the line below:
-- DROP DATABASE IF EXISTS cloudvault;
