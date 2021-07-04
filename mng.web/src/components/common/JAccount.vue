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

        <Modal v-model="loginDialog" :loading="true" width="360" @on-ok="doLogin()" ref="loginDialog">
            <table>
                <tr><td>{{'actName'|i18n}}</td><td><input type="input"  v-model="actName"/></td></tr>
                <tr><td>{{'Password'|i18n}}</td><td><input type="password"  v-model="pwd"/></td></tr>

                <tr>
                    <td><input type="checkbox"  v-model="rememberPwd" @change="rememberPwdChange()"/>{{'RememberPwd'|i18n}}</td>
                    <td><a href="javascript:void(0)" @click="resetPasswordEmail()">{{'ResetPassword'|i18n}}</a></td>
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

    </div>
</template>

<script>

    import rpc from "@/rpc/rpcbase";
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

            email:null,
            mobile:null,
            actName : null,
            pwd : null,
            confirmPwd : null,
            oldPwd:null,

            actInfo : null,
            isLogin:false,
            msg:'',

            rememberPwd:true,
        };
    },

    mounted(){
        let self = this;
        rpc.addActListener(cid,(type,ai)=>{
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

        this.rememberPwd = localStorage.get("rememberPwd");

        this.actName = localStorage.get("actName");
        this.pwd = localStorage.get("pwd");

        if(this.rememberPwd || !this.actName || this.actName.endWith('guest_')) {
            this.doLogin();
        }
    },

    methods: {
        doLoginOrLogout(){
            if(this.actInfo) {
                this.doLogout();
            } else {
               this.actName = localStorage.get("actName"),
               this.pwd = localStorage.get("pwd"),
               this.rememberPwd = localStorage.get("rememberPwd");
               this.loginDialog = true;
            }
        },

        rememberPwdChange(){
            localStorage.set("rememberPwd",this.rememberPwd);
            if(this.rememberPwd) {
                localStorage.set("actName",this.actName);
                localStorage.set("pwd",this.pwd);
            }else {
                localStorage.remove("pwd");
                localStorage.remove("actName");
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
                this.actName = null;
                this.pwd = null;
                this.confirmPwd = null;
                this.oldPwd = null;
                this.msg = null;
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

            let self = this;
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

        doLogin(){
            let self = this;
            this.$refs.loginDialog.buttonLoading = false;

            if(!this.pwd) {
               this.pwd = "";
            }

            if(!this.actName) {
                this.actName = localStorage.get("actName");
            }

            self.msg = '';
            rpc.login(this.actName,this.pwd,(actInfo,err)=>{
                if(!err && actInfo) {
                    self.actInfo = actInfo;
                    self.isLogin = true;
                    self.msg = '';
                    self.loginDialog = false;
                    //window.jm.vue.$emit('userLogin',actInfo);
                } else {
                    self.isLogin = false;
                    self.msg = err || 'Login fail';
                }
            });
        },

        doLogout(){
            let self = this;
            rpc.logout((sus,err)=>{
                if(!err && sus) {
                    self.actInfo = null;
                    self.msg = '';
                    self.isLogin = false;
                    //window.jm.vue.$emit('userLogout');
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
