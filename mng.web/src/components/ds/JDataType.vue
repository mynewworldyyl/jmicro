<template>
	<div class="JDataType">
		<el-table :data="list" style="width: 100%" row-key="id" border :lazy="true" :load="getChildren"
			:tree-props="{children: 'children', hasChildren: 'leaf'}"
			:fit="true" row-class-name="drow"  size="mini"
			:height="($jcontentHeight-32-33.04)+'px'"
			:header-cell-style="{'text-align':'left'}"
			:cell-style="getCellStyle">
		 <el-table-column  align="left" :cell-style="{'text-align':'left'}">
			 <template slot="header" slot-scope="scope">
			 	{{'name'|i18n}}
			 </template>
			 <template slot-scope="scope">
			 	{{scope.row.name}}
			 </template>
			</el-table-column>
			<el-table-column width="180">
				<template slot="header" slot-scope="scope">
					{{'Key'|i18n}}
				</template>
				<template slot-scope="scope">
					{{scope.row.key}}
				</template>
			</el-table-column>
			<el-table-column width="60">
				<template slot="header" slot-scope="scope">
					{{'Enable'|i18n}}
				</template>
				<template slot-scope="scope">
					{{scope.row.enable}}
				</template>
			</el-table-column>
			<el-table-column width="60">
				<template slot="header" slot-scope="scope">
					{{'Sort'|i18n}}
				</template>
				<template slot-scope="scope">
					{{scope.row.sort}}
				</template>
			</el-table-column>
			<el-table-column width="80"> 
				<template slot="header" slot-scope="scope">
					{{'ID'|i18n}}
				</template>
				<template slot-scope="scope">
					{{scope.row.id}}
				</template>
			</el-table-column>
			<el-table-column  width="120">
				<template slot="header" slot-scope="scope">
					{{'clientId'|i18n}}
				</template>
				<template slot-scope="scope">
					{{scope.row.clientId}}
				</template>
			</el-table-column>
			<el-table-column width="120">
				<template slot="header" slot-scope="scope">
					{{'createdBy'|i18n}}
				</template>
				<template slot-scope="scope">
					{{scope.row.createdBy}}
				</template>
			</el-table-column>
			
			 <el-table-column align="right" width="180">
			  <template slot="header" slot-scope="scope">
			   {{'Operation'|i18n}}
			  </template>
			  <template slot-scope="scope">
				<a size="mini"  @click="view(scope.row)">{{'View'|i18n}}</a>&nbsp;
				<a size="mini" @click="add(scope)">{{'Add'|i18n}}</a>&nbsp;
				<a size="mini"  @click="update(scope.row)">{{'Edit'|i18n}}</a>&nbsp;
				<a size="mini"  @click="deleteItem(scope)">{{'Delete'|i18n}}</a>
			  </template>
			</el-table-column>
			
		</el-table>


		<div v-if="isLogin && list && list.length > 0" style="position:relative;text-align:center;">
			<Page ref="pager" :total="totalNum" :page-size="qry.pageSize" :current="qry.curPage" show-elevator
				show-sizer show-total @on-change="curPageChange" @on-page-size-change="pageSizeChange"
				:page-size-opts="[10, 30, 60,100]"></Page>
		</div>

		<div v-if="!isLogin">{{msg}}</div>

		<div v-if="isLogin  && (!list || list.length == 0)">{{msg}}</div>

		<!--  创建 或 更新 -->
		<Drawer v-model="addDrawer.drawerStatus" :closable="false" placement="right" :transfer="true" :draggable="true"
			:scrollable="true" width="50" :mask-closable="false">
			<div class="error">{{msg}}</div>
			<a v-if="isLogin" @click="doAdd()">{{'Confirm'|i18n}}</a>&nbsp;&nbsp;
			<a @click="addDrawer.drawerStatus=false">{{'Cancel'|i18n}}</a>
			<div>
				<Label for="Pid">{{'Pid'|i18n}}</Label>
				<Input v-if="updateModel" :disabled="true" id="Pid" v-model="ic.pid" />
				<Input v-else-if="ptype" :disabled="true" id="Pid" v-model="ptype.id" />
				<Input v-else :disabled="true" id="Pid" />

				<Label for="enable">{{'Enable'|i18n}}</Label>
				<checkbox id="enable" v-model="ic.enable" /><BR/>
				
				<Label for="name">{{'name'|i18n}}</Label>
				<Input id="name" v-model="ic.name" />
				
				<Label for="Key">{{'Key'|i18n}}</Label>
				<Input id="Key" v-model="ic.key" />

				<Label for="desc">{{'desc'|i18n}}</Label>
				<Input id="desc" v-model="ic.desc" />
				
				<Label for="attr0">{{'attr0'|i18n}}</Label>
				<Input id="attr0" v-model="ic.attr0" />

				<Label for="attr1">{{'attr1'|i18n}}</Label>
				<Input id="attr1" v-model="ic.attr1" />

				<Label for="attr2">{{'attr2'|i18n}}</Label>
				<Input id="attr2" v-model="ic.attr2" />
				
				<Label for="sort">{{'Sort'|i18n}}</Label>
				<Input id="sort" v-model="ic.sort" />
				
				<Label v-if="isAdmin" for="clientId">{{'clientId'|i18n}}</Label>
				<Select v-if="isAdmin" v-model="ic.clientId">
					<Option v-for="(val,key) in clients" :key="key" :value="val">{{val}}</Option>
				</Select>

				<Label for="createdTime">{{'createdTime'|i18n}}</Label>
				<div id="createdTime">{{ic.createdTime| formatDate(1)}}</div>

				<Label for="updatedTime">{{'updatedTime'|i18n}}</Label>
				<div id="updatedTime">{{ic.updatedTime| formatDate(1)}}</div>

				<Label for="id">{{'id'|i18n}}</Label>
				<Input :disabled="true" id="id" v-model="ic.id" />

			</div>

		</Drawer>
		
		<!--  创建 或 更新 -->
		<Drawer v-model="viewDrawer.drawerStatus" :closable="false" placement="right" :transfer="true" :draggable="true"
			:scrollable="true" width="50">
			<a @click="viewDrawer.drawerStatus=false">{{'Confirm'|i18n}}</a>
			<div>
				<Label for="name">{{'name'|i18n}}</Label>
				<Input :disabled="true" id="name" v-model="ic.name" />
				
				<Label for="Key">{{'Key'|i18n}}</Label>
				<Input :disabled="true" id="Key" v-model="ic.key" />
		
				<Label for="Desc">{{'Desc'|i18n}}</Label>
				<Input :disabled="true" id="Desc" v-model="ic.desc" />
		
				<Label for="Pid">{{'Pid'|i18n}}</Label>
				<Input :disabled="true" id="Pid" v-model="ic.pid" />
		
				<Label for="attr0">{{'attr0'|i18n}}</Label>
				<Input :disabled="true" id="attr0" v-model="ic.attr0" />
		
				<Label for="attr1">{{'attr1'|i18n}}</Label>
				<Input  :disabled="true" id="attr1" v-model="ic.attr1" />
		
				<Label for="attr2">{{'attr2'|i18n}}</Label>
				<Input  :disabled="true" id="attr2" v-model="ic.attr2" />
		
				<Label for="sort">{{'Sort'|i18n}}</Label>
				<Input :disabled="true" id="sort" v-model="ic.sort" />
				
				<Label for="enable">{{'Enable'|i18n}}</Label>
				<checkbox  :disabled="true" id="enable" v-model="ic.enable" /><BR/>
		
				<Label  for="clientId">{{'clientId'|i18n}}</Label>
				<Input  :disabled="true" id="clientId" v-model="ic.clientId" />
		
				<Label for="createdTime">{{'createdTime'|i18n}}</Label>
				<div   :disabled="true" id="createdTime">{{ic.createdTime| formatDate(1)}}</div>
		
				<Label for="updatedTime">{{'updatedTime'|i18n}}</Label>
				<div  :disabled="true" id="updatedTime">{{ic.updatedTime| formatDate(1)}}</div>
		
				<Label for="id">{{'id'|i18n}}</Label>
				<Input :disabled="true" id="id" v-model="ic.id" />
		
			</div>
		
		</Drawer>

		<div v-if="isLogin" :style="qryDrawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openDrawer()">
		</div>

		<Drawer v-if="isLogin" v-model="qryDrawer.drawerStatus" :closable="false" placement="left" :transfer="true"
			:draggable="true" :scrollable="true" width="50">
			<table id="queryTable">
				<td>{{'Parent'|i18n}}</td>
				<td>
					<i-button @click="openTypeDialog()">{{'Select'|i18n}}</i-button>
					<i-button @click="qryDrawer.drawerStatus=false">{{'Cancel'|i18n}}</i-button>
				</td>
				</tr>
				<tr>
					<td>{{'Name'|i18n}}</td>
					<td>
						<Input v-model="qry.ps.name" />
					</td>
					<td>{{'Attr0'|i18n}}</td>
					<td>
						<Input v-model="qry.ps.attr0" />
					</td>
				</tr>
				<tr>
					<td>{{'Attr1'|i18n}}</td>
					<td>
						<Input v-model="qry.ps.attr1" />
					</td>
					<td>{{'Attr2'|i18n}}(*)</td>
					<td>
						<Input v-model="qry.ps.attr2" />
					</td>
				</tr>
				
				<tr>
					<td>{{'Enable'|i18n}}</td>
					<td>
						<checkbox v-model="qry.ps.enable" />
					</td>
					<td>{{'Key'|i18n}}</td>
					<td>
						<Input v-model="qry.ps.key" />
					</td>
				</tr>
				
				<tr>
					<td></td>
					<td>
					</td>
					<td> </td>
					<td>
						<i-button @click="doQuery()">{{'Query'|i18n}}</i-button>
					</td>
				</tr>
			</table>
		</Drawer>

		<el-dialog title="类型选择" :visible.sync="dialogVisible" width="30%" center>
			<div class="tree-wrapper">
				<el-tree :data="list" :props="props" :load="getChildren" node-key='id' lazy show-checkbox
					@check="selectParent">
				</el-tree>
			</div>
			<div class="btn-wrapper">
				<i-button @click="">{{'Confirm'|i18n}}</i-button>
				<i-button @click="">{{'Cancel'|i18n}}</i-button>
			</div>
		</el-dialog>

	</div>
</template>

<script>
	import lc from "@/rpc/localStorage"
	const cid = 'jdataType'
	
	const resoMap = {}

	export default {
		name: cid,
		components: {},

		data() {
			return {
				dialogVisible: false,
				props: {
					label: 'name',
					children: 'children',

				},

				msg: '',
				actInfo: null,
				totalNum: 0,
				ptype:{},
				ic: {enable:true},

				qry: {
					size: 30,
					curPage: 1,
					sortName: 'createdTime',
					order: "desc",
					ps: {enable:true}
				},

				clients: [],
				isLogin: false,
				isAdmin: false,
				list: [],

				updateModel: false,

				addDrawer: {
					drawerStatus: false,
					drawerBtnStyle: {
						left: '0px',
						zindex: 1000
					},
				},

				qryDrawer: {
					drawerStatus: false,
					drawerBtnStyle: {
						left: '0px',
						zindex: 1000
					},
				},
				
				viewDrawer: {
					drawerStatus: false,
					drawerBtnStyle: {
						left: '0px',
						zindex: 1000
					},
				},
			}
		},

		methods: {

			getCellStyle({ row, column, rowIndex, columnIndex }) {
				if(columnIndex == 0) {
					return {'text-align':'left','padding-left':'10px'}
				}else {
					return {'text-align':'left'}
				}
			},
			
			view(c) {
				this.viewDrawer.drawerStatus=true
				this.ic = c
			},
			
			add(scope) {
				console.log(scope)
				if(scope) {
					this.ptype = scope.row
				}else {
					this.ptype = null
				}
				this.ic = {}
				this.updateModel = false;
				this.errMsg = '';
				this.addDrawer.drawerStatus = true;
			},

			update(c) {
				this.updateModel = true
				this.errMsg = ''
				this.ic = c
				this.addDrawer.drawerStatus = true
			},

			selectParent(nodeData, selectObj) {
				console.log(selectObj)
			},

			openTypeDialog() {
				this.dialogVisible = true
				if (!this.list) {
					this.getChildren()
				}
			},

			openDrawer() {
				this.qryDrawer.drawerStatus = true;
				this.qryDrawer.drawerBtnStyle.zindex = 10000;
				this.qryDrawer.drawerBtnStyle.left = '0px';
			},

			doQuery() {
				this.qry.curPage = 1
				//this.qry.ps = []
				this.refresh()
			},

			async deleteItem(scope) {
				console.log(scope)
				let c = scope.row
			   this.$confirm('此操作将永久删除 [ ' + c.name + ' ], 是否继续?', '提示', {
				  confirmButtonText: '确定',
				  cancelButtonText: '取消',
				  type: 'warning'
				}).then( () => {
					this.doDeleteItem(c)
				}).catch(() => {
				});
			},
			
			async doDeleteItem(c) {
				let resp = await this.$jr.rpc.invokeByCode(-2115314231, [c.id])
				console.log(resp)
				if (resp.code == 0) {
					console.log(resp)
					//服务器删除成功，从其父节点删除
					this.$message({message: '删除成功',type: 'success' });
					if(c.pid == 0) {
						//删除树根
						for(let i = 0; i < this.list.length; i++) {
							if(this.list[i].id == c.id) {
								this.list.splice(i,1)
								break;
							}
						}
					}else if(c.parent && c.parent.children && c.parent.children.length > 0) {
						console.log(resp)
						for(let i = 0; i < c.parent.children.length; i++) {
							if(c.parent.children[i].id == c.id) {
								c.parent.children.splice(i,1)
								if(resoMap[c.parent.id]) {
									resoMap[c.parent.id](c.parent.children)
								}
								break;
							}
						}
						
					}else if(c.parent){
						c.parent.leaf = false
						//console.log(resp)
						this.$message.success("删除成功")
					}
				} else {
					//console.log(resp)
					this.$message.error(resp.msg)
				}
			},

			async clientList() {
				if (!this.isAdmin) {
					return
				}

				let resp = await this.$jr.rpc.invokeByCode(-946676778, [{
					size: 20,
					curPage: 0,
					ps: {}
				}])
				if (resp.code == 0) {
					this.clients = resp.data
				} else {
					console.log(resp)
				}
			},

			async getChildren(parentNode, treeNode, resolve) {
				resoMap[parentNode.id] = resolve
				let pid = parentNode ? parentNode.id : 0
				let resp = await this.$jr.rpc.invokeByCode(-1428697610, [pid,/*this.qry.ps.enable*/null])
				if(resp.code == 0) {
					if (resp.data && resp.data.length > 0) {
						let ar = this.parseChildren(resp.data,parentNode)
						if (parentNode) {
							parentNode.children.push(...ar)
							if(resolve) {
								resolve(ar)
							}
						} else {
							this.list = ar
							return ar
						}
					}else {
						parentNode.leaf = false //无子结点，更新为叶子结点
					}
				} else {
					this.$message.info(resp.msg)
				}
				return []
			},
			
			parseChildren(nodes,p) {
				if(!nodes || nodes.length == 0) return []
				nodes.forEach(e=>{
					e.parent = p
					if(!e.leaf) {
						e.children=[]
					}
					e.leaf = !e.leaf
				})
				return nodes
			},

			async doAdd() {
				let self = this

				if (!this.ic.name) {
					this.msg = '资源名称不能为空'
					return;
				}

				console.log(this.ic)
				if (this.updateModel) {
						let ic = {}
						for(let k in this.ic) {
							if(!(k == 'parent' || k == 'children')) {
								ic[k] = this.ic[k]
							}
						}
						let resp = await this.$jr.rpc.invokeByCode(-536264251, [ic])
						if(resp.code != 0) {
							this.$message.error(resp.msg)
						}
					
				} else {
					//add
					if(this.ptype) {
						this.ic.pid = this.ptype.id
					}
					let resp = await this.$jr.rpc.invokeByCode(2006146623, [this.ic])
					//console.log(resp)
					//console.log(this.ptype)
					let a = this.parseChildren([resp.data],this.ptype)
					if(this.ptype) {
						if(!this.ptype.children) {
							this.ptype.children = []
						}
						if(!this.ptype.leaf) {
							this.ptype.leaf = true
						}
						this.ptype.children.push(a[0])
						if(resoMap[this.ptype.id]) {
							console.log(this.ptype)
							resoMap[this.ptype.id](this.ptype.children)
						}
					} else {
						this.list.push(a[0])
						//console.log(this.list)
					}
				}
				this.addDrawer.drawerStatus = false
			},

			async refresh() {
				this.actInfo = this.$jr.auth.actInfo
				this.isLogin = this.$jr.auth.isLogin()
				this.isAdmin = this.actInfo.isAdmin

				if (this.isLogin) {
					this.list = []
					let qry = this.getQueryConditions();
					let resp = await this.$jr.rpc.invokeByCode(-427086612, [qry])
					this.list = this.parseChildren(resp.data,null);
					console.log(this.list)
					if (this.totalNum != resp.total) {
						this.totalNum = resp.total;
					}
				} else {
					this.list = [];
					this.$Notice.warning({
						title: 'Error',
						desc: '未登录',
					});
				}
			},

			getQueryConditions() {
				return this.qry;
			},

			pageSizeChange(pageSize) {
				this.qry.pageSize = pageSize;
				this.qry.curPage = 1;
				this.refresh();
			},

			curPageChange(curPage) {
				this.qry.curPage = curPage
				this.refresh()
			},

		},

		mounted() {

			//this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
			this.$jr.auth.addActListener(() => {
				this.refresh();
			});
			let self = this;

			this.$bus.$emit("editorOpen", {
				"editorId": cid,
				"menus": [{
						name: "Add",
						label: "Add",
						icon: "ios-cog",
						call: self.add
					},
					{
						name: "Refresh",
						label: "Refresh",
						icon: "ios-cog",
						call: self.refresh
					}
				]
			});

			let ec = function() {
				this.$jr.auth.removeActListener(cid);
				this.$off('editorClosed', ec);
			}

			this.refresh();

			this.$bus.$on('editorClosed', ec);
		},

		beforeDestroy() {
			this.$jr.auth.removeActListener(cid);
		},

	}
</script>

<style>
	.JDataType {}

	.drow{
		height:28px;
	}
	.tree-wrapper {
		height: 280px;
		width: 200px;
	}
</style>
