<template>
    <div class="accountStatuBar">
        <span class="loginBtn" href="javascript:void(0);" @click="doLoginOrLogout()">
           {{ actInfo && actInfo.success  ? 'Logout':'Login'}}
        </span>

        <span v-if="!actInfo  || !actInfo.success" class="registBtn" href="javascript:void(0);" @click="regist()">
           {{ 'Regist'}}
        </span>

        <span v-if="actInfo && actInfo.success" class="accountBtn" href="javascript:void(0);" @click="changePwd()">
            {{actInfo.actName}}</span>

        <Modal v-model="loginDialog" :loading="true" width="360" @on-ok="doLogin()" ref="loginDialog">
            <table>
                <tr><td>actName</td><td><input type="input"  v-model="actName"/></td></tr>
                <tr><td>Password</td><td><input type="password"  v-model="pwd"/></td></tr>
                <!--<tr><td>confirm Password</td><td><input type="password"  v-model="cfPwd"/></td></tr>-->
                <tr><td colspan="2">{{msg}}</td></tr>
            </table>
        </Modal>

        <Modal v-model="registDialog" :loading="true" width="360" @on-ok="doRegist()" ref="registDialog">
            <table>
                <tr><td>actName</td><td><input type="input"  v-model="actName"/></td></tr>
                <tr><td>Password</td><td><input type="password"  v-model="pwd"/></td></tr>
                <tr><td>confirm Password</td><td><input type="password"  v-model="confirmPwd"/></td></tr>
                <tr><td colspan="2">{{msg}}</td></tr>
            </table>
        </Modal>

        <Modal v-model="changePwdDialog" :loading="true" width="360" @on-ok="doChangePwd()" ref="changePwdDialog">
            <table>
                <tr><td>actName</td><td><input type="input"  readonly="true" v-model="actInfo.actName"/></td></tr>
                <tr><td>Old Password</td><td><input type="password"  v-model="oldPwd"/></td></tr>
                <tr><td>Password</td><td><input type="password"  v-model="pwd"/></td></tr>
                <tr><td>confirm Password</td><td><input type="password"  v-model="confirmPwd"/></td></tr>
                <tr><td colspan="2">{{msg}}</td></tr>
            </table>
        </Modal>

    </div>
</template>

<script>

export default {
    name: 'JAccount',

    components: {

    },

    data() {
        return {
            loginDialog : false,
            registDialog: false,
            changePwdDialog: false,

            actName : null,
            pwd : null,
            confirmPwd : null,
            oldPwd:null,

            actInfo : {actName:'',success:false},
            msg:''
        };
    },

    methods: {
        doLoginOrLogout(){
            if(this.actInfo.success) {
                this.doLogout();
            } else {
               this.actName = window.jm.localStorage.get("actName"),
               this.pwd = window.jm.localStorage.get("pwd"),
               this.loginDialog = true;
            }
        },

        changePwd() {
            this.reset();
            this.changePwdDialog = true;
        },

        doChangePwd() {
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

            if(this.pwd != this.oldPwd) {
                this.msg = "New password equals to old password!";
                return;
            }

            this.actInfo.pwd = this.pwd;

            this.msg = "";


            let self = this;
            window.jm.mng.act.updatePwd(this.pwd,this.oldPwd,(state,errmsg)=>{
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

            if(!this.confirmPwd) {
                this.msg = "confirm password cannot be NULL!";
                return;
            }

            if(this.pwd != this.confirmPwd) {
                this.msg = "Confirm password is not equal password!";
                return;
            }
            this.msg = "";

            let self = this;
            window.jm.mng.act.regist(this.actName,this.pwd,(state,errmsg)=>{
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
                self.msg = 'Invalid pwd';
                return;
            }
            self.msg = '';
            window.jm.rpc.login(this.actName,this.pwd,(actInfo,err)=>{
                if(!err && actInfo.success) {
                    self.actInfo = actInfo;
                    self.msg = '';
                    this.loginDialog = false;
                    window.jm.localStorage.set("actName",self.actName);
                    window.jm.localStorage.set("pwd",self.pwd);
                    //window.jm.vue.$emit('userLogin',actInfo);
                }else {
                    self.msg = err.msg | 'Login fail';
                }
            });
        },

        doLogout(){
            let self = this;
            window.jm.rpc.logout((sus,err)=>{
                if(!err && sus) {
                    self.actInfo = { actName:'',success:false };
                    self.msg = '';
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
