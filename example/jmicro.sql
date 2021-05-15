/*
 Navicat Premium Data Transfer

 Source Server         : localmysql
 Source Server Type    : MySQL
 Source Server Version : 50725
 Source Host           : localhost:3306
 Source Schema         : jmicro

 Target Server Type    : MySQL
 Target Server Version : 50725
 File Encoding         : 65001

 Date: 19/04/2021 16:34:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_good
-- ----------------------------
DROP TABLE IF EXISTS `t_good`;
CREATE TABLE `t_good`  (
  `id` int(11) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `price` decimal(10, 2) NULL DEFAULT NULL,
  `total` int(10) NULL DEFAULT NULL,
  `usable_cnt` int(10) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_good
-- ----------------------------
INSERT INTO `t_good` VALUES (1, 'good', 1000.00, 100, 78);

-- ----------------------------
-- Table structure for t_order
-- ----------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order`  (
  `id` int(11) NOT NULL,
  `good_id` int(11) NULL DEFAULT NULL,
  `num` int(11) NULL DEFAULT NULL,
  `amount` decimal(10, 0) NULL DEFAULT NULL,
  `txid` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_order
-- ----------------------------
INSERT INTO `t_order` VALUES (1473, 1, 1, 1000, 883);
INSERT INTO `t_order` VALUES (1477, 1, 1, 1000, 885);
INSERT INTO `t_order` VALUES (1479, 1, 1, 1000, 886);
INSERT INTO `t_order` VALUES (1483, 1, 1, 1000, 888);
INSERT INTO `t_order` VALUES (1485, 1, 1, 1000, 889);
INSERT INTO `t_order` VALUES (1489, 1, 1, 1000, 891);
INSERT INTO `t_order` VALUES (1492, 1, 1, 1000, 893);
INSERT INTO `t_order` VALUES (1495, 1, 1, 1000, 894);
INSERT INTO `t_order` VALUES (1497, 1, 1, 1000, 895);
INSERT INTO `t_order` VALUES (1501, 1, 1, 1000, 897);
INSERT INTO `t_order` VALUES (1503, 1, 1, 1000, 898);
INSERT INTO `t_order` VALUES (1507, 1, 1, 1000, 900);
INSERT INTO `t_order` VALUES (1509, 1, 1, 1000, 901);

-- ----------------------------
-- Table structure for t_payment
-- ----------------------------
DROP TABLE IF EXISTS `t_payment`;
CREATE TABLE `t_payment`  (
  `id` int(11) NOT NULL,
  `order_id` int(11) NULL DEFAULT NULL,
  `amount` decimal(10, 0) NULL DEFAULT NULL,
  `txid` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_payment
-- ----------------------------
INSERT INTO `t_payment` VALUES (1474, 1473, 1000, 883);
INSERT INTO `t_payment` VALUES (1478, 1477, 1000, 885);
INSERT INTO `t_payment` VALUES (1480, 1479, 1000, 886);
INSERT INTO `t_payment` VALUES (1484, 1483, 1000, 888);
INSERT INTO `t_payment` VALUES (1486, 1485, 1000, 889);
INSERT INTO `t_payment` VALUES (1490, 1489, 1000, 891);
INSERT INTO `t_payment` VALUES (1493, 1492, 1000, 893);
INSERT INTO `t_payment` VALUES (1496, 1495, 1000, 894);
INSERT INTO `t_payment` VALUES (1498, 1497, 1000, 895);
INSERT INTO `t_payment` VALUES (1502, 1501, 1000, 897);
INSERT INTO `t_payment` VALUES (1504, 1503, 1000, 898);
INSERT INTO `t_payment` VALUES (1508, 1507, 1000, 900);
INSERT INTO `t_payment` VALUES (1510, 1509, 1000, 901);

-- ----------------------------
-- Table structure for t_req
-- ----------------------------
DROP TABLE IF EXISTS `t_req`;
CREATE TABLE `t_req`  (
  `txid` int(11) NOT NULL,
  `goodId` int(11) NULL DEFAULT NULL,
  `num` int(5) NULL DEFAULT NULL,
  PRIMARY KEY (`txid`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_req
-- ----------------------------
INSERT INTO `t_req` VALUES (883, 1, 1);
INSERT INTO `t_req` VALUES (885, 1, 1);
INSERT INTO `t_req` VALUES (886, 1, 1);
INSERT INTO `t_req` VALUES (888, 1, 1);
INSERT INTO `t_req` VALUES (889, 1, 1);
INSERT INTO `t_req` VALUES (893, 1, 1);
INSERT INTO `t_req` VALUES (894, 1, 1);
INSERT INTO `t_req` VALUES (895, 1, 1);
INSERT INTO `t_req` VALUES (897, 1, 1);
INSERT INTO `t_req` VALUES (898, 1, 1);
INSERT INTO `t_req` VALUES (900, 1, 1);
INSERT INTO `t_req` VALUES (901, 1, 1);

SET FOREIGN_KEY_CHECKS = 1;
