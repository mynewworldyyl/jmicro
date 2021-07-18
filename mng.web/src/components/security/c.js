const STATUS_APPLY = 1  //审核中
const STATUS_REJECT = 2  //拒绝
const STATUS_APPROVE = 3 //通过
const STATUS_REVOKE = 4  //已回收

const statusLabels = {}
statusLabels[STATUS_APPLY]='(审核中)';
statusLabels[STATUS_REJECT]='(拒绝)';
statusLabels[STATUS_APPROVE]='(通过)';
statusLabels[STATUS_REVOKE]='(已回收)';

const TYPE_ROLE = 1;//授权给角色
const TYPE_ACT = 2;//授权给账号

function  parseActPermissionData(role) {
    let pl = [];
    if(!role.pers) {
        role.pers= [];
    }
    for(let modelKey in role.permissionEntires ) {
        let srcPs = role.permissionEntires[modelKey];
        if(!srcPs || srcPs.length == 0) {
            continue;
        }

        let children = [];
        pl.push({ title: modelKey, children:children});

        for(let i = 0; i < srcPs.length; i++) {
            let sp = srcPs[i];
            role.pers.push(sp.haCode);

            let label = sp.label + statusLabels[sp.status];

            let e = {
                title : sp.label,
                expanded : true,
                srcData : sp,
                render: (h) => {
                    return h('span',[
                        h('span',{
                            style:{ marginLeft: '10px' }
                        },label)
                    ]);
                }
            }
            children.push(e);
        }
    }
    role.permissionParseEntires = pl;
    return pl;
}

function parsePermissionData(self) {
    let pl = [];

    for(let modelKey in self.srcPermissions) {
        let srcPs = self.srcPermissions[modelKey];
        if(!srcPs || srcPs.length == 0) {
            continue;
        }

        let children = [];
        pl.push({ title: modelKey, children:children});

        let isCheck = (haCode) => {
            if(self.role.pers) {
                for(let i = 0; i < self.role.pers.length; i++) {
                    if(haCode == self.role.pers[i]) {
                        return true;
                    }
                }
            }
            return false;
        }

        for(let i = 0; i < srcPs.length; i++) {
            let sp = srcPs[i];
            let e = {
                title: sp.label,
                srcData : sp,
                expand: true,
                checked: isCheck(sp.haCode),
                render: (h/*,params*/) => {
                    return h('span',[
                        h('span',{
                            style:{ marginLeft: '10px' }
                        },sp.label)

                    ]);
                }
            }
            children.push(e);
        }
    }
    return pl;
}

export default {
    TYPE_ROLE,
    TYPE_ACT,
    STATUS_APPLY,
    STATUS_REJECT,
    STATUS_APPROVE,
    STATUS_REVOKE,
    statusLabels,
    parseActPermissionData,
    parsePermissionData,
}
