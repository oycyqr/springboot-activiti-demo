CREATE TABLE `holiday` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(100) DEFAULT NULL,
  `userName` varchar(80) DEFAULT NULL,
  `reason` varchar(200) DEFAULT NULL,
  `startTime` datetime DEFAULT NULL,
  `endTime` datetime DEFAULT NULL,
  `type` tinyint(1) DEFAULT NULL,
  `status` tinyint(1) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `ext1` varchar(100) DEFAULT NULL,
  `ext2` varchar(100) DEFAULT NULL,
  `ext3` varchar(100) DEFAULT NULL,
  `ext4` varchar(100) DEFAULT NULL,
  `ext5` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8
