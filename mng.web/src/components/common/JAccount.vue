<template>
    <div class="accountStatuBar">
        <span v-if="actInfo" class="loginBtn" href="javascript:void(0);" @click="doLoginOrLogout()">
           {{ 'Logout'|i18n}}
        </span>
        <span v-if="!actInfo" class="loginBtn" href="javascript:void(0);" @click="doLoginOrLogout()">
           {{ 'Login'|i18n}}
        </span>
        <span v-if="!actInfo" class="registBtn" href="javascript:void(0);" @click="regist()">
           {{ 'Regist'|i18n}}
        </span>
        <span v-if="actInfo != null && !actInfo.guest " class="accountBtn" href="javascript:void(0);" @click="changePwd()">
            {{actInfo != null ? actInfo.actName:''}}
        </span>

        <span v-if="actInfo != null && actInfo.guest" class="accountBtn" href="javascript:void(0);" @click="regist()">
            {{actInfo != null ? actInfo.actName:''}}
        </span>

        <span v-if="actInfo != null && actInfo.clientIds && actInfo.clientIds.length > 1" class="accountBtn"
            href="javascript:void(0);" @click="changeClient()">切换
        </span>

        <Modal v-model="loginDialog" :loading="true" width="360" @on-ok="doLogin()" ref="loginDialog">
            <table>
                <tr><td>{{'actName'|i18n}}</td><td><input type="input"  v-model="actName"/></td></tr>
                <tr><td>{{'Password'|i18n}}</td><td><input type="password"  v-model="pwd"/></td></tr>
                <tr v-if="vcodeId"><td>{{'vcode'|i18n}}</td>
					<td>
						<input type="input"  v-model="vcode"/>
						<img :src="codeUrl" @click="getCode()">
					</td>
				</tr>
                <tr>
                    <td><input type="checkbox"  v-model="rememberPwd" @change="rememberPwdChange()"/>{{'RememberPwd'|i18n}}</td>
                    <td>
						<a href="javascript:void(0)" @click="resetPasswordEmail()">{{'ResetPassword'|i18n}}</a>&nbsp;&nbsp;
						<a v-if="showResendEmail" href="javascript:void(0)" @click="sendActiveEmail()">{{'ResendEmail'|i18n}}</a>
					</td>
                </tr>

               <!-- <tr><td>
                    <td><input :disabled="!rememberPwd" type="check"  v-model="autoLogin"/>{{'AutoLogin'|i18n}}</td>
                </tr>-->

                <tr><td colspan="2">{{msg}}</td></tr>
            </table>
        </Modal>

        <Modal v-model="registDialog" :loading="true" width="360" @on-ok="doRegist()" ref="registDialog">
            <table>
                <tr><td>{{'actName'|i18n}}</td><td><input type="input"  v-model="actName"/></td></tr>
                <tr><td>{{'Password'|i18n}}</td><td><input type="password"  v-model="pwd"/></td></tr>
                <tr><td>{{'ConfirmPassword'|i18n}}</td><td><input type="password"  v-model="confirmPwd"/></td></tr>
                <tr><td>{{'Mobile'|i18n}}</td><td><input type="input"  v-model="mobile"/></td></tr>
                <tr><td>{{'Email'|i18n}}</td><td><input type="input"  v-model="email"/></td></tr>
                <tr><td colspan="2" style="color:red;">{{'EmailDesc' |i18n}}</td></tr>
                <tr><td colspan="2">{{msg}}</td></tr>
            </table>
        </Modal>

        <Modal v-model="changePwdDialog" :loading="true" width="360" @on-ok="doUpdatePwd()" ref="changePwdDialog">
            <table>
                <tr><td>{{'actName'|i18n}}</td><td><input v-if="actInfo" type="input"  readonly="true" v-model="actInfo.actName"/></td></tr>
                <tr><td>{{'OldPassword'|i18n}}</td><td><input type="password"  v-model="oldPwd"/></td></tr>
                <tr><td>{{'Password'|i18n}}</td><td><input type="password"  v-model="pwd"/></td></tr>
                <tr><td>{{'ConfirmPassword'|i18n}}</td><td><input type="password"  v-model="confirmPwd"/></td></tr>
                <tr><td colspan="2">{{msg}}</td></tr>
            </table>
        </Modal>

        <Modal v-model="resetPwdDialog" :loading="true" width="360" @on-ok="doResetPwd()" ref="changePwdDialog">
            <table>
                <tr><td>{{'actName'|i18n}}</td><td><input type="input"  readonly="true" v-model="actName"/></td></tr>
                <tr><td>{{'Password'|i18n}}</td><td><input type="password"  v-model="pwd"/></td></tr>
                <tr><td>{{'ConfirmPassword'|i18n}}</td><td><input type="password"  v-model="confirmPwd"/></td></tr>
                <tr><td>{{'CheckCode'|i18n}}</td><td><input type="checkcode"  v-model="checkCode"/></td></tr>
                <tr><td colspan="2">{{msg}}</td></tr>
            </table>
        </Modal>

        <Modal v-model="clientListDialog" :loading="true" width="360" @on-ok="doChangeClient()" ref="clientListDialog">
            <RadioGroup v-model="selectClientId" vertical>
                <Radio v-for="(v,k) in clientList" :value="k" :key="k" :label="k">
                    {{v}}
                </Radio>
            </RadioGroup>
        </Modal>

    </div>
</template>

<script>
	
    import {Constants} from "@/rpc/message";
    import localStorage from "@/rpc/localStorage";
    import act from "@/rpcservice/act";
    import utils from "@/rpc/utils";
    
    const cid = 'JAccount';

export default {
    name: 'JAccount',

    components: {

    },

    data() {
        return {

            loginDialog : false,
            registDialog: false,
            changePwdDialog: false,
            resetPwdDialog:false,
            checkCode:'',

            clientList:[],
            clientListDialog:false,
            selectClientId: 0,

            email:null,
            mobile:null,
            actName : null,
            pwd : null,
            confirmPwd : null,
            oldPwd:null,

            vcode:null,
            vcodeId:null,
            codeUrl:null,

            actInfo : null,
            isLogin:false,
            msg:'',

            rememberPwd: localStorage.get(this.$jr.cons.ACT_AUTO_LOGIN_KEY),
			
			showResendEmail : false
        };
    },

    mounted(){
        let self = this;

		this.$jr.auth.toLoginCb = (codeUrl, vcodeId)=>{
			if(codeUrl) {
				this.codeUrl = 'data:image/gif;base64,' + codeUrl
				this.vcodeId = vcodeId
			}
			
			if(this.actInfo) {
				this.doLogout();
			}
			
			this.doLoginOrLogout()
		}
		
        this.$jr.auth.addActListener((type,ai)=>{
            if(type == Constants.LOGIN) {
                self.actInfo = ai;
                self.isLogin = true;
                self.msg = '';
            }else if(type == Constants.LOGOUT) {
                self.isLogin = false;
                self.actInfo = null;
                self.msg = '';
            }
        });

        this.rememberPwd = localStorage.get(Constants.ACT_REM_PWD_KEY);
        this.actName = localStorage.get(Constants.ACT_NAME_KEY);
        this.pwd = localStorage.get(Constants.ACT_PWD_KEY);

        if(this.rememberPwd || this.actName && this.actName.endWith('guest_')) {
            this.doLogin();
        }

    },

    methods: {
		async sendActiveEmail(){
			let rst = await act.resendActiveEmail(this.actName)
			if(rst.code == 0) {
				 this.showResendEmail = false
				 this.$Message.info("邮件发送成功，请打开注册邮箱激活账号");
			} else {
				 this.$Message.info(rst.msg || "邮件发送失败");
			}
		},
		
        doChangeClient(){
            //let req = this.$jr.this.$jr.rpccreq(act.sn,act.ns,act.v,'changeCurClientId',[this.selectClientId])
            this.$jr.rpc.callRpcWithParams(act.sn,act.ns,act.v,'changeCurClientId',[this.selectClientId])
                .then((resp)=>{
                    if(resp && resp.code == 0) {
                        this.actInfo = resp.data
                        this.clientListDialog = false
                        this.selectClientId = this.actInfo.clientId+""
                        this.$jr.auth.setActInfo(resp.data)
                    } else {
                        console.log(resp);
                    }
                }).catch((err)=>{
                console.log(err);
            });
        },

        changeClient(){
            //let req = this.$jr.rpccreq(act.sn,act.ns,act.v,'clientList',[])
            this.$jr.rpc.callRpcWithParams(act.sn,act.ns,act.v,'clientList',[])
                .then(( resp )=>{
                    if(resp && resp.code == 0) {
                        this.selectClientId = this.actInfo.clientId+""
                        this.clientList = resp.data
                        this.clientListDialog = true
                    } else {
                        console.log(resp);
                    }
                }).catch((err)=>{
                    console.log(err);
            });
        },

        getCode(){
            this.$jr.auth.getCode(1).then(resp => { // 生成验证码图片
                if(resp.code == 0) {
                    this.codeUrl = 'data:image/gif;base64,' + resp.data
                    this.vcodeId = resp.msg
                }else {
                    this.msg = resp.msg;
                }
            })
        },

        doLoginOrLogout(){
            if(this.$jr.auth.isLogin()) {
                this.doLogout();
            } else {
               //this.getCode();
               this.actName = localStorage.get(Constants.ACT_NAME_KEY),
               this.pwd = localStorage.get(Constants.ACT_PWD_KEY),
               this.rememberPwd = localStorage.get(Constants.ACT_REM_PWD_KEY);
               this.loginDialog = true;
            }
        },

        rememberPwdChange(){
            localStorage.set(Constants.ACT_REM_PWD_KEY,this.rememberPwd);
			localStorage.set(this.$jr.cons.ACT_AUTO_LOGIN_KEY,this.rememberPwd)
            if(this.rememberPwd) {
                localStorage.set(Constants.ACT_NAME_KEY,this.actName);
                localStorage.set(Constants.ACT_PWD_KEY,this.pwd);
            }else {
                localStorage.remove(Constants.ACT_PWD_KEY);
                localStorage.remove(Constants.ACT_NAME_KEY);
            }
        },

        changePwd() {
            this.reset();
            this.changePwdDialog = true;
        },

        doResetPwd(){
            if(!this.actName || this.actName.length == 0) {
                this.msg = "Account name cannot be NULL";
                return;
            }

            if(!this.pwd || this.pwd.length == 0) {
                this.msg = "New password cannot be NULL";
            }

            if(!this.confirmPwd || this.confirmPwd.length == 0) {
                this.msg = "confirm password cannot be NULL!";
                return;
            }

            if(!this.pwd || this.pwd.length == 0) {
                this.msg = "New password cannot be NULL";
            }

            if(!this.checkCode || this.checkCode.length == 0) {
                this.msg = "Check code cannot be null!";
                return;
            }

            let self = this;
            act.resetPwd(this.actName,this.checkCode,this.pwd)
            .then((resp)=>{
				if(resp.code != 0 ) {
					self.$Message.error(resp.msg);
					return;
				}

                self.resetPwdDialog = false;
                self.reset();
            }).catch((err)=>{
                window.console.log(err);
                if(err && err.errorCode && err.msg) {
                    self.msg = err.msg
                } else {
                    self.$Message.error(err);
                }
            });
        },

        resetPasswordEmail(){
            if(!this.actName || this.actName.length == 0) {
                this.msg = "Account name cannot be NULL";
                return;
            }

            let self = this;
            act.resetPwdEmail(this.actName,"0")
                .then((resp)=>{
                    if(resp.code != 0 ) {
                        self.msg = resp.msg;
                        return;
                    }
                    self.msg = '验证码已经发往你注册时使用的邮箱，请前往邮箱查获取验证码';
                    self.checkCode = '';
                    self.loginDialog = false;
                    self.resetPwdDialog = true
                }).catch((err)=>{
                    window.console.log(err);
                    if(err && err.errorCode && err.msg) {
                        self.msg = err.msg
                    }else {
                        self.$Message.error(err);
                    }
            });
        },

        doUpdatePwd() {
            this.$refs.changePwdDialog.buttonLoading = false;
            if(!this.oldPwd) {
                this.msg = "Old password cannot be NULL";
            }

            if(!this.pwd) {
                this.msg = "New password cannot be NULL";
            }

            if(!this.confirmPwd) {
                this.msg = "confirm password cannot be NULL!";
                return;
            }

            if(this.pwd != this.confirmPwd) {
                this.msg = "Confirm password is not equal to new password!";
                return;
            }

            if(this.pwd == this.oldPwd) {
                this.msg = "New password equals to old password!";
                return;
            }

            this.actInfo.pwd = this.pwd;

            this.msg = "";

            let self = this;
            act.updatePwd(this.pwd,this.oldPwd,(state,errmsg)=>{
                if(state) {
                    self.$Message.success("Update password successfully")
                    self.changePwdDialog = false;
                    self.reset();
                    self.doLogout();
                }else {
                    self.msg = errmsg;
                }
            });
        },

        regist() {
            this.reset();
            this.registDialog = true;
        },

        reset() {
                this.actName = null
                this.pwd = null
                this.confirmPwd = null
                this.oldPwd = null
                this.msg = null
                this.vcode = null
                this.vcodeId = null
        },

        doRegist() {
            this.$refs.registDialog.buttonLoading = false;
            if(!this.actName) {
                this.msg = "Account name cannot be NULL";
                return;
            }

            if(!this.pwd) {
                this.msg = "Password cannot be NULL";
            }

            if(!this.email) {
                this.msg = "Email cannot be NULL";
                return;
            }

            if(!this.mobile) {
                this.msg = "Mobile cannot be NULL";
            }

            if(!this.confirmPwd) {
                this.msg = "confirm password cannot be NULL!";
                return;
            }

            if(this.pwd != this.confirmPwd) {
                this.msg = "Confirm password is not equal password!";
                return;
            }

            if(!utils.checkEmail(this.email)) {
                this.msg = "Email format invalid!";
                return;
            }

            if(!utils.checkMobile(this.mobile)) {
                this.msg = "Mobile number format invalid!";
                return;
            }

            this.msg = "";

            let self = this
            act.regist(this.actName,this.pwd,this.email,this.mobile,(state,errmsg)=>{
                if(state) {
                    self.$Message.success("Regist successfully")
                    self.registDialog = false;
                    self.reset();
                }else {
                    self.msg = errmsg;
                }
            });
        },

        login(actName,pwd,vcode,vcodeId,cb){
            if(this.$jr.auth.isLogin() && cb) {
                this.actInfo = this.$jr.auth.actInfo
                cb(this.actInfo,null);
                return;
            }

            this.actInfo = null;

            if(!actName) {
                actName = localStorage.get(Constants.ACT_NAME_KEY);
                let rememberPwd = localStorage.get(Constants.ACT_REM_PWD_KEY);
                if(rememberPwd || actName /*|| actName.startWith("guest_")*/) {
                    let pwd = localStorage.get(Constants.ACT_PWD_KEY);
                    if(!pwd) {
                        pwd="";
                    }
                }
            }

            if(!actName) {
                //自动建立匿名账号
                actName = localStorage.get(Constants.ACT_GUEST_NAME_KEY);
                if(!actName) {
                    actName = "";
                }
            }

            if(!pwd) {
                pwd = ""
            }
			
			 let self = this
			 this.$jr.auth.login(actName,pwd,vcode,vcodeId,false)
			.then(( resp )=>{
				if(resp.code == 0) {
					self.codeUrl =null
					self.vcodeId = null
					
					if(this.$jr.auth.actInfo.actName.startWith("guest_")) {
						localStorage.set(Constants.ACT_GUEST_NAME_KEY,this.$jr.auth.actInfo.actName)
					}
		
					if(cb)
						cb(this.$jr.auth.actInfo,null)
				} else if(resp.code == 4) {
					self.codeUrl = 'data:image/gif;base64,' + resp.vcode
					self.vcodeId = resp.vcodeId
				}else {
					if(resp.key && resp.key == '1') {
						self.showResendEmail = true
					}
					if(cb) {
						cb(null,resp.msg)
					}
				}
			}).catch((err)=>{
					if(err && err.code == 4) {
						let arr = err.msg.split('$@$')
						self.codeUrl = 'data:image/gif;base64,' + arr[0]
						self.vcodeId = arr[1]
					} else {
						console.log(err)
						if(cb) {
							cb(null,err)
						}
					}
			});
                
        },

        doLogin(){
            let self = this
            this.$refs.loginDialog.buttonLoading = false

            if(!this.pwd) {
               this.pwd = ""
            }

            if(!this.actName) {
                this.actName = localStorage.get(Constants.ACT_NAME_KEY)
            }

            self.msg = ''
            self.login(this.actName,this.pwd,this.vcode,this.vcodeId,
				(actInfo,err)=>{
                self.vcode = null
                self.vcodeId = null
                self.codeUrl=null

                if(!err && actInfo) {
                    self.actInfo = actInfo
                    self.isLogin = true
                    self.msg = ''
                    self.loginDialog = false
                    //this.$bus.$emit('userLogin',actInfo);
                } else {
                    self.isLogin = false
                    self.msg = err || 'Login fail'
                    self.getCode()
                }
            });
        },

        doLogout(){
            let self = this;
            this.$jr.auth.logout((sus,err)=>{
                if(!err && sus) {
                    self.actInfo = null;
                    self.msg = '';
                    self.isLogin = false;
                    //this.$bus.$emit('userLogout');
                } else {
                    self.msg = 'Login fail';
                }
            });
        }

    },
}

</script>

<style scoped>
    .accountStatuBar{
        position: absolute;
        right: 10px;
        top:0px;
        z-index: 1000;
        height: 35px;
    }

    .loginBtn, .accountBtn, .registBtn {
        height:35px;
        text-align: center;
        line-height: 35px;
        display:inline-block;
        padding:0 5px;
        cursor: pointer;
        right: 0px;
        top:0px;
        color: blue;
    }

    .registBtn,.accountBtn{
        right: 38px;
        margin-left: 10px;
    }

</style>
