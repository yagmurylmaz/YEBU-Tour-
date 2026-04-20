-- MySQL dump 10.13  Distrib 9.6.0, for macos15.7 (arm64)
--
-- Host: localhost    Database: yebu_hotel
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cities`
--

DROP TABLE IF EXISTS `cities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cities` (
  `id` int NOT NULL AUTO_INCREMENT,
  `country_id` int NOT NULL,
  `name` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_city_country` (`country_id`,`name`),
  KEY `idx_city_country` (`country_id`),
  CONSTRAINT `fk_city_country` FOREIGN KEY (`country_id`) REFERENCES `countries` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cities`
--

LOCK TABLES `cities` WRITE;
/*!40000 ALTER TABLE `cities` DISABLE KEYS */;
INSERT INTO `cities` VALUES (2,1,'Ankara'),(8,1,'Çanakkale'),(7,1,'Erzurum'),(1,1,'Istanbul'),(3,2,'Berlin'),(4,4,'Istanbul'),(5,5,'Ardahan'),(6,6,'asd');
/*!40000 ALTER TABLE `cities` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `countries`
--

DROP TABLE IF EXISTS `countries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `countries` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `countries`
--

LOCK TABLES `countries` WRITE;
/*!40000 ALTER TABLE `countries` DISABLE KEYS */;
INSERT INTO `countries` VALUES (6,'Aforika Borwa'),(4,'Benin'),(2,'Germany'),(5,'Iraq'),(3,'Syria'),(1,'Turkey');
/*!40000 ALTER TABLE `countries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `extra_services`
--

DROP TABLE IF EXISTS `extra_services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `extra_services` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(512) NOT NULL DEFAULT '',
  `price` decimal(12,2) NOT NULL,
  `billing_type` varchar(32) NOT NULL DEFAULT 'PER_NIGHT',
  `active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `extra_services`
--

LOCK TABLES `extra_services` WRITE;
/*!40000 ALTER TABLE `extra_services` DISABLE KEYS */;
INSERT INTO `extra_services` VALUES (1,'BREAKFAST','Breakfast','Open buffet breakfast',120.00,'PER_NIGHT',1),(2,'GYM','Gym','Unlimited gym access',90.00,'PER_NIGHT',1),(3,'POOL','Pool','Indoor pool access',75.00,'PER_NIGHT',1),(4,'PARKING','Parking','Reserved parking spot',60.00,'PER_NIGHT',1);
/*!40000 ALTER TABLE `extra_services` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hotels`
--

DROP TABLE IF EXISTS `hotels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hotels` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `country_id` int NOT NULL,
  `city_id` int NOT NULL,
  `address_line` varchar(512) NOT NULL,
  `phone` varchar(64) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `image_path` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_hotel_country` (`country_id`),
  KEY `idx_hotel_city` (`city_id`),
  CONSTRAINT `fk_hotel_city` FOREIGN KEY (`city_id`) REFERENCES `cities` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_hotel_country` FOREIGN KEY (`country_id`) REFERENCES `countries` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hotels`
--

LOCK TABLES `hotels` WRITE;
/*!40000 ALTER TABLE `hotels` DISABLE KEYS */;
INSERT INTO `hotels` VALUES (6,'The Erzurum Hotel',1,7,'Erzurum merkez mahallesi','','','2026-04-19 20:05:04','/Users/efeinan/.hotel-app/hotel-images/hotel-6-20232573056208.png'),(7,'Avec Hotel',1,8,'çanakkale lapseki','','','2026-04-19 20:09:25','/Users/efeinan/.hotel-app/hotel-images/hotel-7-20365246584958.png');
/*!40000 ALTER TABLE `hotels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password_reset_tokens`
--

DROP TABLE IF EXISTS `password_reset_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_tokens` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `token_hash` char(64) NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_prt_token` (`token_hash`),
  UNIQUE KEY `uq_prt_user` (`user_id`),
  CONSTRAINT `fk_prt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_tokens`
--

LOCK TABLES `password_reset_tokens` WRITE;
/*!40000 ALTER TABLE `password_reset_tokens` DISABLE KEYS */;
INSERT INTO `password_reset_tokens` VALUES (2,3,'c239a84c1c66252062377812e05104ab18257b734039a6e2996019db3664e6fd','2026-04-19 12:08:37','2026-04-19 14:08:36'),(16,2,'b78e35e06745482840a3d588f2c0df9f7971619afc35609aa0b55a6618e4b6c4','2026-04-19 14:54:38','2026-04-19 16:54:37');
/*!40000 ALTER TABLE `password_reset_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservation_services`
--

DROP TABLE IF EXISTS `reservation_services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservation_services` (
  `id` int NOT NULL AUTO_INCREMENT,
  `reservation_id` int NOT NULL,
  `service_code` varchar(64) NOT NULL,
  `service_name` varchar(128) NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `billing_type` varchar(32) NOT NULL DEFAULT 'PER_NIGHT',
  `line_total` decimal(14,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_rs_reservation` (`reservation_id`),
  CONSTRAINT `fk_rs_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservation_services`
--

LOCK TABLES `reservation_services` WRITE;
/*!40000 ALTER TABLE `reservation_services` DISABLE KEYS */;
/*!40000 ALTER TABLE `reservation_services` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservations`
--

DROP TABLE IF EXISTS `reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservations` (
  `id` int NOT NULL AUTO_INCREMENT,
  `customer_id` int NOT NULL,
  `room_id` int NOT NULL,
  `check_in` date NOT NULL,
  `check_out` date NOT NULL,
  `total_price` decimal(14,2) NOT NULL,
  `status` varchar(32) NOT NULL,
  `created_at` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_res_user` (`customer_id`),
  KEY `fk_res_room` (`room_id`),
  CONSTRAINT `fk_res_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_res_user` FOREIGN KEY (`customer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservations`
--

LOCK TABLES `reservations` WRITE;
/*!40000 ALTER TABLE `reservations` DISABLE KEYS */;
INSERT INTO `reservations` VALUES (1,3,2,'2026-04-18','2026-04-19',2220.00,'APPROVED','2026-04-18 17:28'),(2,3,7,'2026-04-19','2026-04-20',123.00,'PENDING','2026-04-19 18:09');
/*!40000 ALTER TABLE `reservations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_images`
--

DROP TABLE IF EXISTS `room_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_images` (
  `id` int NOT NULL AUTO_INCREMENT,
  `room_id` int NOT NULL,
  `image_path` varchar(1024) NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ri_room` (`room_id`),
  KEY `idx_ri_room_sort` (`room_id`,`sort_order`),
  CONSTRAINT `fk_ri_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_images`
--

LOCK TABLES `room_images` WRITE;
/*!40000 ALTER TABLE `room_images` DISABLE KEYS */;
INSERT INTO `room_images` VALUES (1,7,'/Users/efeinan/.hotel-app/room-images/room-7-13596401515958.jpeg',0,'2026-04-19 17:48:48'),(2,7,'/Users/efeinan/.hotel-app/room-images/room-7-13596426198875.jpg',1,'2026-04-19 17:48:48'),(3,8,'/Users/efeinan/.hotel-app/room-images/room-8-16046779709291.jpg',0,'2026-04-19 18:29:38'),(4,9,'/Users/efeinan/.hotel-app/room-images/room-9-18828279183416.jpeg',0,'2026-04-19 19:16:00'),(5,10,'/Users/efeinan/.hotel-app/room-images/room-10-19594343083875.webp',0,'2026-04-19 19:56:34'),(6,10,'/Users/efeinan/.hotel-app/room-images/room-10-19626093463166.png',1,'2026-04-19 19:57:06'),(7,11,'/Users/efeinan/.hotel-app/room-images/room-11-20281217000166.png',0,'2026-04-19 20:08:01'),(8,12,'/Users/efeinan/.hotel-app/room-images/room-12-20396635657541.png',0,'2026-04-19 20:09:57');
/*!40000 ALTER TABLE `room_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rooms`
--

DROP TABLE IF EXISTS `rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `id` int NOT NULL AUTO_INCREMENT,
  `room_number` varchar(64) NOT NULL,
  `room_type` varchar(32) NOT NULL,
  `price_per_night` decimal(12,2) NOT NULL,
  `capacity` int NOT NULL,
  `description` varchar(1024) NOT NULL DEFAULT '',
  `available` tinyint(1) NOT NULL DEFAULT '1',
  `image_path` varchar(1024) DEFAULT NULL,
  `hotel_id` int DEFAULT NULL,
  `available_from` date DEFAULT NULL,
  `available_to` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_room_hotel` (`hotel_id`),
  CONSTRAINT `fk_room_hotel` FOREIGN KEY (`hotel_id`) REFERENCES `hotels` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rooms`
--

LOCK TABLES `rooms` WRITE;
/*!40000 ALTER TABLE `rooms` DISABLE KEYS */;
INSERT INTO `rooms` VALUES (2,'102','DOUBLE',2100.00,2,'Comfort double room',1,NULL,NULL,'2026-01-01','2026-12-31'),(3,'201','SUITE',3400.00,3,'Sea view suite',1,NULL,NULL,'2026-01-01','2026-12-31'),(4,'301','DELUXE',4200.00,4,'Deluxe family room',1,NULL,NULL,'2026-01-01','2026-12-31'),(6,'192','SINGLE',1999.00,2,'deneme',1,'/Users/efeinan/.hotel-app/room-images/room-6.jpeg',NULL,'2026-01-01','2026-12-31'),(7,'1232141','SINGLE',123.00,4123,'124',1,'/Users/efeinan/.hotel-app/room-images/room-7-13596401515958.jpeg',NULL,'2026-01-01','2026-12-31'),(8,'akdsfnads','SINGLE',12333.00,124213,'asdasd',1,'/Users/efeinan/.hotel-app/room-images/room-8-16046779709291.jpg',NULL,'2026-01-01','2026-12-31'),(9,'102','DELUXE',3000.00,2,'medium',1,'/Users/efeinan/.hotel-app/room-images/room-9-18828279183416.jpeg',NULL,'2026-01-01','2026-12-31'),(10,'12321','DOUBLE',2.00,3,'3',1,'/Users/efeinan/.hotel-app/room-images/room-10-19594343083875.webp',NULL,'2026-01-01','2026-12-31'),(11,'101','DOUBLE',3000.00,2,'standart room',1,'/Users/efeinan/.hotel-app/room-images/room-11-20281217000166.png',6,'2026-01-01','2026-12-31'),(12,'201','DOUBLE',3210.00,2,'canakkale bea',1,'/Users/efeinan/.hotel-app/room-images/room-12-20396635657541.png',7,'2026-01-01','2026-12-31');
/*!40000 ALTER TABLE `rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone` varchar(64) NOT NULL,
  `role` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'System Admin','admin@hotel.com','sha256$Betr6DgjXBrtPnpiK++USg==$eEmpmHivNeF0TUWUxjZj0GwB2wur0wgqCjRZCSYcdvM=','0000000000','ADMIN'),(2,'efe inan','efeinan05@gmail.com','sha256$yOwe0VhfSN17OT/zmoCbPA==$o8yH4mIhZpGstcfZJTYQKIaY485gsClfs4MlS+R7G3g=','+90 541 599 02 33','CUSTOMER'),(3,'efe inan','efeinan04@gmail.com','sha256$qq311/3kCkbs3GNGd4gD7g==$pD6QsJb3Vd+Bnc3NcxgGirndfYrI4M3Ze0It56ggT+A=','+90 541 599 02 33','CUSTOMER');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'yebu_hotel'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-19 21:58:39
