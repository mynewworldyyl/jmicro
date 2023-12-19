import ps from '../pubsub.js'

const orderStatus = {
		STATUS_INVALID : 0,
	
		//已下单但还未支付
		STATUS_CREATE : 1,
		
		//未支付前主动取消
		STATUS_CANCEL  : 2,
		
		//超时未支付取消
		STATUS_AUTO_CANCEL  : 3,

		//订单已支付
		STATUS_PAYED  : 5,

		//商家备货打包中
		STATUS_GOODS_PREPARING  : 10,

		//订单分发中，待配送员接单
		STATUS_RECEIVE_WAITING  : 15,

		//配送员已经接单，待取件
		STATUS_WAIT_GETTING  : 20,

		//配送员已经取件，配送中
		STATUS_DISPATCHING  : 25,

		//配送员已送达
		STATUS_DISPATCHING_SUCCESS  : 30,

		//配送员送达失败
		STATUS_DISPATCHING_FAIL  : 31,

		//配送员送货到客户处，客户拒收，配送退回
		STATUS_DISPATCHING_REJECT: 32,

		//买家确认订单
		STATU_COMFIRM_GET  : 35,

		//已送达订单超时未确认
		STATU_COMFIRM_GET_TIMEOUT : 36,

		//退货
		STATUS_REFUND  : 40,
		
		//退货确认成功
		STATUS_REFUND_CONFIRM : 41,

		//确认取消订单，取消已经接单及之后的订单，需要对方同意，无条件同意取消
		STATUS_COMFIRM_CANCEL : 42,

		//确认取消订单，取消已经接单及之后的订单，需要对方同意并得到适当赔偿，有条件同意取消
		STATUS_COMFIRM_CANCEL_WITH_COND : 43,
}

const status2Desc = {}
status2Desc[orderStatus.STATUS_CREATE]='未支付'
status2Desc[orderStatus.STATUS_CANCEL]='取消'
status2Desc[orderStatus.STATUS_AUTO_CANCEL]='超时未支付取消'
status2Desc[orderStatus.STATUS_PAYED]='已支付'
status2Desc[orderStatus.STATUS_GOODS_PREPARING]='备货打包中'
status2Desc[orderStatus.STATUS_RECEIVE_WAITING]='待接单'
status2Desc[orderStatus.STATUS_WAIT_GETTING]='待取件'
status2Desc[orderStatus.STATUS_DISPATCHING]='配送中'
status2Desc[orderStatus.STATUS_DISPATCHING_SUCCESS]='已送达'
status2Desc[orderStatus.STATUS_DISPATCHING_FAIL]='未支付'
status2Desc[orderStatus.STATUS_DISPATCHING_FAIL]='配送失败'
status2Desc[orderStatus.STATU_COMFIRM_GET]='已签收'
status2Desc[orderStatus.STATU_COMFIRM_GET_TIMEOUT]='超时签收'
status2Desc[orderStatus.STATUS_REFUND]='退货'
status2Desc[orderStatus.STATUS_REFUND_CONFIRM]='退货确认'
status2Desc[orderStatus.STATUS_COMFIRM_CANCEL]='取消成功'
status2Desc[orderStatus.STATUS_COMFIRM_CANCEL_WITH_COND]='附条件取消'
status2Desc[orderStatus.STATUS_DISPATCHING_REJECT]='附条件取消'
status2Desc[orderStatus.STATUS_DISPATCHING_REJECT]='客户拒收'
status2Desc[orderStatus.STATUS_INVALID]='无效'

//优惠费用项目
const CAC = {
	//订单总额 正数
	CAC_ORDER : 1,
		
	//总运费 正项或0
	CAC_FREIGHT : 2,
		
	//优惠券优惠 负项或0
	CAC_COUPON : 10,
		
	//店铺满减优惠 负项或0
	CAC_SHOP_MANJIAN : 12,
		
	//团购优惠
	CAC_GROUPON : 14,
	
	//积分扣减
	CAC_INTEGRAL : 16,
	
	CAC_FLASHSALES : 17,
	
	//小计
	CAC_HUIZONG : 126,
	
	//实际支付
	CAC_PAYED : 127,
}

const cac2Desc = {}
cac2Desc[CAC.CAC_ORDER] = "订单总额"
cac2Desc[CAC.CAC_FREIGHT] = "运费"
cac2Desc[CAC.CAC_COUPON] = "优惠券优惠"
cac2Desc[CAC.CAC_SHOP_MANJIAN] = "店铺满减优惠"
cac2Desc[CAC.CAC_GROUPON] = "团购优惠"
cac2Desc[CAC.CAC_INTEGRAL] = "积分扣减"
cac2Desc[CAC.CAC_HUIZONG] = "小计"
cac2Desc[CAC.CAC_FLASHSALES] = "秒杀优惠"
cac2Desc[CAC.CAC_PAYED] = "实际支付"
cac2Desc[CAC.CAC_HUIZONG] = "小计"

// 优惠券状态
const COU_STATUS = {
	COU_STATUS_NORMAL:1,//正常
	COU_STATUS_EXPIRE:2,//过期
	COU_STATUS_OFF:3,//下架
}

const couStatus2Desc = {}
couStatus2Desc[COU_STATUS.COU_STATUS_NORMAL] = "正常"
couStatus2Desc[COU_STATUS.COU_STATUS_EXPIRE] = "过期"
couStatus2Desc[COU_STATUS.COU_STATUS_OFF] = "下架"

// 优惠券投放类型
const COU_TRANS = {
	COU_TRANS_REGIST:1,//注册赠送
	COU_TRANS_GET:2,//用户领取
	COU_TASK_GIVEN:3,//任务奖励
	//COU_TRANS_BEFORE_PAY:4,//下单支付前发放(满减)
	COU_TASK_AFTER_BUY:5,//消费后发放(元购)
	COU_GIVE_BY_ADMIN:6,//手动发放
}

const couTrans2Desc = {}
couTrans2Desc[COU_TRANS.COU_TRANS_REGIST] = "注册赠送"
couTrans2Desc[COU_TRANS.COU_TRANS_GET] = "用户领取"
couTrans2Desc[COU_TRANS.COU_TASK_GIVEN] = "任务奖励"
//couTrans2Desc[COU_TRANS.COU_TRANS_BEFORE_PAY] = "满减"
couTrans2Desc[COU_TRANS.COU_TASK_AFTER_BUY] = "消费后发放"
couTrans2Desc[COU_TRANS.COU_GIVE_BY_ADMIN] = "手动发放"

//优惠券使用范围
const COU_RANGE = {
	COU_RANGE_ALL:1,//全场通用
	//COU_RANGE_SHOP:2,//店铺通用
	COU_RANGE_CATEGORY:3,//指定分类
	COU_RANGE_GOODS:4,//指定商品
}

const couRange2Desc = {}
couRange2Desc[COU_RANGE.COU_RANGE_ALL] = "全场通用"
//couRange2Desc[COU_RANGE.COU_RANGE_SHOP] = "店铺通用"
couRange2Desc[COU_RANGE.COU_RANGE_CATEGORY] = "指定分类"
couRange2Desc[COU_RANGE.COU_RANGE_GOODS] = "指定商品"

//优惠券优惠方式
const COU_TYPE = {
	COU_TYPE_DEC:1,//满减
	COU_TYPE_SHOP:2,//折扣
}

//优惠券类型
const couType2Desc = {}
couType2Desc[COU_TYPE.COU_TYPE_DEC] = "优惠金额(元)"
couType2Desc[COU_TYPE.COU_TYPE_SHOP] = "折扣百分比"

//优惠券有效期类型
const COU_VALID = {
	COU_VALID_DAYS:1,//领券天数
	COU_VALID_START_END:2,//领券超始时间
}

//优惠券有效期类型
const couValid2Desc = {}
couValid2Desc[COU_VALID.COU_VALID_DAYS] = "领券天数"
couValid2Desc[COU_VALID.COU_VALID_START_END] = "起始时间"

//分享类型
const SHARE_TYPE = {
	SHARE_TYPE_GOODS:1,//商品
	SHARE_TYPE_SHOP:2,//店铺
	SHARE_TYPE_GROUPON:3,//团购
}
const shareType2Desc = {}
shareType2Desc[SHARE_TYPE.SHARE_TYPE_GOODS] = "商品"
shareType2Desc[SHARE_TYPE.SHARE_TYPE_SHOP] = "店铺"
shareType2Desc[SHARE_TYPE.SHARE_TYPE_GROUPON] = "团购"

const PERIOD_TYPE = {
	//PERIOD_ONE : 1, //一次性
	PERIOD_HALF_OF_HOUR : 2, //按小时重复
	PERIOD_HOUR : 3, //按小时重复
	PERIOD_DAY : 4, //按天重复
	PERIOD_WEEK : 5, //按周重复
	PERIOD_MONTH : 6, //按月重复
	PERIOD_SEASON : 7, //按季度重复
	PERIOD_YEAR : 8, //按年重复
}

const periodType2Desc = {}
//periodType2Desc[PERIOD_TYPE.PERIOD_ONE] = "一次性"
periodType2Desc[PERIOD_TYPE.PERIOD_HALF_OF_HOUR] = "每半小时重复"
periodType2Desc[PERIOD_TYPE.PERIOD_HOUR] = "每小时重复"
periodType2Desc[PERIOD_TYPE.PERIOD_DAY] = "每天重复"
periodType2Desc[PERIOD_TYPE.PERIOD_WEEK] = "每周重复"
periodType2Desc[PERIOD_TYPE.PERIOD_MONTH] = "每月重复"
//periodType2Desc[PERIOD_TYPE.PERIOD_SEASON] = "按季度重复"
//periodType2Desc[PERIOD_TYPE.PERIOD_YEAR] = "按年重复"

const periodType2Interval = {}

//periodType2Interval[PERIOD_TYPE.PERIOD_ONE] = 0
periodType2Interval[PERIOD_TYPE.PERIOD_HALF_OF_HOUR] = 30*60*1000
periodType2Interval[PERIOD_TYPE.PERIOD_HOUR] = 1*60*60*1000
periodType2Interval[PERIOD_TYPE.PERIOD_DAY] = 24*60*60*1000
periodType2Interval[PERIOD_TYPE.PERIOD_WEEK] = 7*24*60*60*1000
periodType2Interval[PERIOD_TYPE.PERIOD_MONTH] = 30*24*60*60*1000
//periodType2Interval[PERIOD_TYPE.PERIOD_SEASON] = 3*30*24*60*60*1000
//periodType2Interval[PERIOD_TYPE.PERIOD_YEAR] = 365*24*60*60*1000

//计算分拥金额类型，分为折扣和绝对金额
const DISTRIBUTION_TYPE = {
	DISTRIBUTION_TYPE_AMOUNT : 1,//绝对金额
	DISTRIBUTION_TYPE_PERCENT : 2, //百分比
}
const distributionType2Desc = {}
distributionType2Desc[DISTRIBUTION_TYPE.DISTRIBUTION_TYPE_AMOUNT] = "绝对金额"
distributionType2Desc[DISTRIBUTION_TYPE.DISTRIBUTION_TYPE_PERCENT] = "百分比"

//用户优惠券状态
const COU_USER_STATUS = {
	COU_USER_STATUS_NOTUSE : 4,//未使用
	COU_USER_STATUS_USED : 1,//已经使用
	COU_USER_STATUS_EXPIRED : 2,//过期
	COU_USER_STATUS_OUT : 3//下架
}

const couUserStatus2Desc = {}
couUserStatus2Desc[COU_USER_STATUS.COU_USER_STATUS_NOTUSE] = "未使用"
couUserStatus2Desc[COU_USER_STATUS.COU_USER_STATUS_USED] = "已使用"
couUserStatus2Desc[COU_USER_STATUS.COU_USER_STATUS_EXPIRED] = "已过期"
couUserStatus2Desc[COU_USER_STATUS.COU_USER_STATUS_OUT] = "已下架"

const GROUPON_TYPE={
	//1:非团购买；
	GROUPON_TYPE_WITHOUT: 1,
	
	//2：加入已经有团购；
	GROUPON_TYPE_ADD: 2,
	
	 //3：开新团购
	GROUPON_TYPE_CREATED: 3,
	
	//秒杀购
	GROUPON_TYPE_FLASHSALES: 4,
}

const groupType2Desc = {}
groupType2Desc[GROUPON_TYPE.GROUPON_TYPE_WITHOUT] = "普通购"
groupType2Desc[GROUPON_TYPE.GROUPON_TYPE_ADD] = "入团购"
groupType2Desc[GROUPON_TYPE.GROUPON_TYPE_CREATED] = "开团购"
groupType2Desc[GROUPON_TYPE.GROUPON_TYPE_FLASHSALES] = "秒杀购"

let d = {
	ROLE_CUST:"cust",
	ROLE_SENDER : "sender",
	ROLE_BRD : "brd",
	SHOP:'__shop',
	
	QR_TYPE_JM : 1,//火山二维码
	QR_TYPE_WX : 2,//微信二维码
	QR_TYPE_AL : 3,//支付宝二维码
	
	//...SHARE_TYPE,
	shareType2Desc,
	
	//...GROUPON_TYPE,
	groupType2Desc,
	
	//订单状态
	//...orderStatus,
	status2Desc,
	
	MS_DAY: 1*24*60*60*1000,
	
	//优惠费用项目
	//...CAC,
	cac2Desc,
	
	//优惠券状态
	//...COU_STATUS,
	couStatus2Desc,
	
	//优惠券投放类型
	//...COU_TRANS,
	couTrans2Desc,
	
	//优惠券使用范围
	//...COU_RANGE,
	couRange2Desc,
	
	//优惠券类型
	//...COU_TYPE,
	couType2Desc,
	
	//优惠券有效期类型
	//...COU_VALID,
	couValid2Desc,
	
	//...PERIOD_TYPE,
	periodType2Desc,
	periodType2Interval,
	
	//...DISTRIBUTION_TYPE,
	distributionType2Desc,
	
	//...COU_USER_STATUS,
	couUserStatus2Desc,

    FREIGHT_TYPE_KUAIDI : 3,//快递
    FREIGHT_TYPE_ZITI : 2,//自提
    FREIGHT_TYPE_PEISONG : 1,//本地配送
    freight2Desc : { 1:'本地配送', 2:'自提', 3:'快递'},

	MSG_TYPE_CHAT_CODE : ps.MSG_TYPE_CHAT_CODE,//聊天消息
	MSG_TYPE_SENDER_CODE : ps.MSG_TYPE_SENDER_CODE,//配送员端订单消息
	MSG_TYPE_CUST_CODE : ps.MSG_TYPE_CUST_CODE, //配送服务客户端消息

	MODEL_TYPE_GOODS: 0,
	//如果type=0，则是商品ID；如果type=1，则是专题ID
	MODEL_TYPE_TOPIC: 1,
	MODEL_TYPE_SHOP: 2,

	//商家或店铺
	//订单信息： 0，全部订单； 1，有效订单； 2，失效订单； 3，结算订单； 4，待结算订单。
	SHOW_TYPE_ALL: 0,
	SHOW_TYPE_VALID: 1,
	SHOW_TYPE_INVALID: 2,
	SHOW_TYPE_SETTLED: 3,
	SHOW_TYPE_SETTLING: 4,
	
	//草稿状态
	SHOP_STATUS_INIT : 1,

	//待审批状态
	SHOP_STATUS_WAIT_APPROVE : 2,

	//审批通过
	SHOP_STATUS_APPROVED : 3,

	//拒绝
	SHOP_STATUS_APPROVE_REJECT : 4,

	//冻结
	SHOP_STATUS_FREEZE : 5,

	//驳回
	SHOP_STATUS_BOHUI : 6,

	//永久注销
	SHOP_STATUS_CLOSE : 7,

	ShopStatus2Desc : {1:'草稿状态', 2:'待审批状态', 3:'审批通过', 4:'拒绝', 5:'冻结', 6:'驳回', 7:'永久注销'},

	IDEN_TAGS : ['Email',"Mobile","Face","RealName"],
	
	ERR:{
		//配送员未上线，但是上传位置信息
		EC_PT_OFFLINE : 32767,
	}

}

 let ddata = [
	SHARE_TYPE,
	GROUPON_TYPE,
	orderStatus,
	CAC,
	COU_STATUS,
	COU_TRANS,
	COU_RANGE,
	COU_TYPE,
	COU_VALID,
	PERIOD_TYPE,
	DISTRIBUTION_TYPE,
	COU_USER_STATUS
	]
	
ddata.forEach(e=>{
	d = Object.assign(d,e)
})

export default d