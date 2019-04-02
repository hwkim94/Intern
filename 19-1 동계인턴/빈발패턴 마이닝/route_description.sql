-- --------------------------------------------------------
-- 호스트:                          127.0.0.1
-- 서버 버전:                        10.1.34-MariaDB - MariaDB Server
-- 서버 OS:                        Linux
-- HeidiSQL 버전:                  9.4.0.5125
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;


-- cctv_manage 데이터베이스 구조 내보내기
CREATE DATABASE IF NOT EXISTS `cctv_manage` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `cctv_manage`;

-- 테이블 cctv_managel.route_description 구조 내보내기
CREATE TABLE IF NOT EXISTS `route_description` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `camera_id` varchar(200) DEFAULT NULL,
  `pattern_id` int(11) NOT NULL DEFAULT '0',
  `route_id` int(11) DEFAULT NULL,
  `profile_description_id` int(11) DEFAULT NULL,
  `user_id` varchar(50) DEFAULT NULL,
  `object_type` varchar(50) DEFAULT NULL,
  `support` double DEFAULT NULL,
  `length` int(11) DEFAULT NULL,
  `count_avg` double DEFAULT NULL,
  `count_std` double DEFAULT NULL,
  `direction_avg` double DEFAULT NULL,
  `direction_std` double DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2197 DEFAULT CHARSET=utf8;

-- 내보낼 데이터가 선택되어 있지 않습니다.
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
