/* eslint-disable */
import rpc from './rpcbase';
import ls from './localStorage';
import cfg from './config.js';
import utils from './utils.js';

import { Constants } from './message'

const MC = 93593292

const PF_ALI='alipay'
const PF_WX='wxpay'
const PF_YSF='ysfpay'

const ALIQR = "ALI_QR"  //二维码
const ALIPC = "ALI_PC"  //电脑网站
const ALIBAR = "ALI_BAR"  //条码
const ALIAPP = "ALI_APP"  // app
const ALIWAP = "ALI_WAP"  //wap
const ALIJSAPI = "ALI_JSAPI"  //服务窗

const WXNATIVE = "WX_NATIVE"  //扫码
const WXJSAPI = "WX_JSAPI"  //jsapi
const WXBAR = "WX_BAR"  //条码
const WXLITE = "WX_LITE"  //小程序
const WXH5 = "WX_H5"  //H5

const YSFJSAPI = "YSF_JSAPI" //云闪付JS 
const YSFBAR = "YSF_BAR"  //云闪付条码

const AUTOBAR = "AUTO_BAR" //条码聚合支付
const QRCASHIER = "QR_CASHIER" //收银台支付

const PT_ALI_LIST =[ALIQR, ALIPC, ALIBAR, ALIAPP, ALIWAP, ALIJSAPI]
const PT_WX_LIST =[WXNATIVE, WXJSAPI, WXBAR, WXLITE, WXH5]
const PT_YSF_LIST =[YSFJSAPI, YSFBAR]

const PF_ALI_ITEM = {targetPlatform:PF_ALI, label:"支付宝登录账号", accKey:'aliAcc', btnTitle:"支付宝余额宝", ptList:PT_ALI_LIST}
const PF_WX_ITEM =  {targetPlatform:PF_WX, label:"微信OpenId", accKey:'wxOpenId', btnTitle:"微信零钱", ptList:PT_WX_LIST}

export default {
	PF_ALI,
	PF_WX,
	PF_YSF,
	
	PF_ITEM_LIST: [PF_ALI_ITEM, PF_WX_ITEM],
	
	ALIQR, ALIPC, ALIBAR, ALIAPP, ALIWAP, ALIJSAPI,
	
	WXNATIVE, WXJSAPI, WXBAR, WXLITE, WXH5,
	
	YSFJSAPI, YSFBAR,
	
	PT_ALI_LIST,
	PT_WX_LIST,
	PT_YSF_LIST,
	
	getPayway(pf) {
		let pt = QRCASHIER
		let dt = "imgBase64"
		if(utils.isUni()) {
			if(pf == PF_ALI) {
				// #ifdef APP
				pt = ALIAPP
				// #endif
				
				// #ifdef H5

				// #endif
				
			}else if(pf == PF_WX) {
				// #ifdef APP
				pt = WXNATIVE
				// #endif
				
				// #ifdef H5

				// #endif
			}
		} else if(utils.isH5Plus()) {
			//是否是H5 plus
			
		} else {
			//H5浏览器
		}
		return [pt,dt]
	},

	doPay(res,resultUrl) {
		if(res.code != 0) {
			//this.modalDialog({title:"错误",content:res.msg})
			this.errMsg = res.msg
			return
		}
		
		//#ifdef H5
		//暂时不下得H5
		return
		//#endif

		//#ifdef APP
		var EnvUtils = plus.android.importClass("com.alipay.sdk.app.EnvUtils");
		EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX);
		console.log(EnvUtils)
		let pd = res.data//.replace(/\+/g,"%2B")
		//let pd = "alipay_sdk=alipay-sdk-java-dynamicVersionNo&app_id=2021000121661319&biz_content=%7B%22out_trade_no%22%3A%2270501111111S001111119%22%2C%22subject%22%3A%22%E5%A4%A7%E4%B9%90%E9%80%8F%22%2C%22total_amount%22%3A%229.00%22%7D&charset=UTF8&format=json&method=alipay.trade.app.pay&sign=jfLWAB9K8lodYQBGFWf20TcygyqTCXbd05UuaBSuCDHwjjetmF853Ks%2F4un5xXKf5pSXYWWSrZVV%2FTdzrAWPjRPspcX%2BW5JkqWC%2FlG6dNw1bIBElK9oYffyKS3uYcfUCxZg4udbS%2Fq7MBBmOsO2sulij%2FaZm1HnhM0ifofQsmIa%2FPdcyNAhcBak0KNGKS%2Fm75v9%2B1zjOQARQMdLVTcso60k%2BiuLykVt8zed%2Fd8JyMotPXb9dEVwZcENHismcMB9KxSLInL6a7qkX%2BVq%2Fr15V2yguVPfJbjirZFe4lVwQmXUDZFTI0VFciBjkFJQIa7vY2OUbk6I%2Fde08As8BAYbLnw%3D%3D&sign_type=RSA2&timestamp=2022-11-17+21%3A59%3A21&version=1.0"
		console.log('支付参数',pd);
		uni.requestPayment({
			provider: 'alipay', 
			orderInfo:pd,
			success: async (pr) => {
				console.log('支付过程结果',pr);
				if(!pr.rawdata) {
					uni.redirectTo({
						url: resultUrl + '?status=0&orderId=' + osres.data
					});
					return
				}
				let pres = JSON.parse(pr.rawdata)
				if(pres.resultStatus != 9000) {
					console.log('支付失败,',err);
					uni.redirectTo({
						url: resultUrl + '?status=0&orderId=' + osres.data
					});
					return
				}
				uni.redirectTo({
					url: resultUrl + '?status=1&orderId=' + osres.data
				});
			},
			fail: (err) => {
				console.log('支付过程失败',err);
				uni.redirectTo({
					url: resultUrl + '?status=0&orderId=' + osres.data
				});
			},
			complete: (res)=>{
				console.log('支付过程结束',res);
			}
		});	
		//#endif
		
		// #ifdef MP-WEIXIN
		uni.requestPayment({
			timeStamp: payParam.timeStamp,
			nonceStr: payParam.nonceStr,
			package: payParam.packageValue,
			signType: payParam.signType,
			paySign: payParam.paySign,
			success:  () => {
				console.log('支付过程成功');
				uni.redirectTo({
					url: resultUrl + '?status=1&orderId=' + orderId
				});
			},
			fail: (err) => {
				console.log('支付过程失败',err);
				uni.redirectTo({
					url: resultUrl + '?status=0&orderId=' + orderId
				});
			},
			complete: (res)=>{
				console.log('支付过程结束',res);
			}
		});	
		//#endif
		
		
		
	}
    
	
};
