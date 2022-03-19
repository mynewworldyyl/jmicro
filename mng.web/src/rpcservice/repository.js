import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";
import io from "@/rpc/io";

class Uploader{
		/**
		 * @param {Object} srvCode 上传文件服务接口编码
		 */
		constructor(srvCode) {
			this.srvCode = srvCode;
	
			this.upStatis = {
				finishSize: '',
				costTime: '',
				uploadSpeed: '',
				progressVal: 0,
	
				onUpload: false,
				totalLen: 0,
				blockNum: 0,
				dv: null,
				curBlock: 0,
				startTime: 0,
			},
			this.res0 = null
		}
	
		/**
		 * @param {Object} arrayBuffer 二进制数组
		 * @param {Object} cb 上传结果通知回调
		 */
		uploadArrayBuffer(arrayBuffer, res, cb) {
			let self = this
			this.res0 = res
			this.cb = cb
			self.res0.size = buf.byteLength;
			if (self.res0.id && self.res0.id > 0) {
				//rep.updateResource(self.res0,true)
				rpc.invokeByCode(649964272, [self.res0, true])
					.then((resp) => {
						if (resp.code != 0) {
							this.cb({
								code: io.UP_ERROR,
								msg: JSON.stringify(resp)
							})
							return;
						}
						self.res0 = resp.data;
						this.cb({
							code: io.UP_RES,
							res: self.res0
						})
						self.uploadCurBlock(buf)
					}).catch((err) => {
						console.log(err);
						this.cb({
							code: io.UP_ERROR,
							msg: err
						})
					})
			} else {
				//rep.addResource(self.res0)
				rpc.invokeByCode(1655766688, [self.res0])
				.then((resp) => {
					if (resp.code != 0) {
						cb({
							code: io.UP_ERROR,
							msg: JSON.stringify(resp)
						})
						return;
					}
	
					self.res0 = resp.data;
					this.cb({
						code: io.UP_RES,
						res: self.res0
					})
					self.uploadCurBlock(buf);
				}).catch((err) => {
					this.cb({
						code: io.UP_ERROR,
						msg: err
					})
				});
			}
		}
		
		/**
		 * 
		 * @param {Object} file 要上传的文件
		 * @param {Object} cb 上传结果通知回调
		 */
		uploadFile(file, res, cb) {
			if (!file) {
				cb({
					code: io.UP_ERROR,
					msg: '文件不能为空'
				})
				return;
			}
			let self = this
			io.getFileContent(file, res, cb).then((rst) => {
				if(rst.code == self.UP_SUCCESS) {
					self.uploadArrayBuffer(rst.data)
				}else {
					cb(rst)
				}
			}).catch(err => {
				console.log(err);
				if(err.code) {
					cb(err)
				}else {
					this.cb({
						code: io.UP_ERROR,
						msg: err
					})
				}
			});
		}
		
		/**
		 * @param {Object} file 要上传的文件
		 * @param {Object} cb 上传结果通知回调
		 */
		uploadStringFile(fileStr, res, cb) {
			 let ar = utils.toUTF8Array(fileStr)
			 let vw = new DataView(new ArrayBuffer(ar.length), 0, ar.length);
			 for (let i = 0; i < ar.length; i++) {
			     vw.setInt8(i, ar[i]);
			 }
			 this.uploadArrayBuffer(vw.slice(0, ar.length),res,cb)
		}
	
		uploadData(data, blockNum, cb) {
			let self = this;
			//rep.addResourceData(this.res0.id, data, blockNum)
			rpc.invokeByCode(-1175504504, [this.res0.id, data, blockNum],
				Constants.PROTOCOL_BIN, Constants.PROTOCOL_JSON)
			.then((resp) => {
				if (resp.code == 0) {
					cb(true);
				} else {
					cb(false);
				}
			})
			.catch((err) => {
				cb(false, err);
			});
		}
	
		initProgressData(buf) {
			let self = this;
			let totalLen = buf.byteLength;
			self.upStatis.totalLen = totalLen;
			self.upStatis.totalSize = self.getSizeVal(totalLen);
			self.upStatis.onUpload = true;
			self.upStatis.blockNum = parseInt(totalLen / self.res0.blockSize);
			self.upStatis.dv = new DataView(buf, 0, totalLen);
			self.upStatis.curBlock = 0;
			self.upStatis.startTime = new Date().getTime();
		}
	
		uploadCurBlock(buf) {
			let self = this;
			self.initProgressData(buf)
			self.upStatis.finishSize = self.getFinishSize(self.res0.blockSize, self.upStatis.curBlock);
			self.upStatis.costTime = self.getCostTime(self.upStatis.startTime);
			self.upStatis.progressVal = parseInt(self.getProgressVal(self.res0.blockSize, self.upStatis.curBlock, self
				.upStatis.totalLen));
			self.upStatis.uploadSpeed = self.getSpeedVal(self.res0.blockSize, self.upStatis.curBlock, self.upStatis
				.startTime);
	
			if (self.upStatis.curBlock < self.upStatis.blockNum) {
				let bl = [];
				for (let j = 0; j < self.res0.blockSize; j++) {
					bl.push(self.upStatis.dv.getUint8(self.res0.blockSize * self.upStatis.curBlock + j));
				}
				self.uploadData(bl, self.upStatis.curBlock, (success, err) => {
					if (success) {
						self.upStatis.curBlock += 1;
						self.cb({
							code: io.UP_PROGRESS,
							statis: self.upStatis
						}) //更新进度
						self.uploadCurBlock();
					} else {
						self.errMsg = "Fail upload: " + self.res0.name;
						self.resetUploadStatis();
						self.cb({
							code: io.UP_ERROR,
							msg: errMsg,
							err
						}) //报错
					}
				});
			} else if (self.upStatis.curBlock == self.upStatis.blockNum) {
				//最后一块
				let lastBlockSize = self.upStatis.totalLen % self.res0.blockSize;
				if (lastBlockSize > 0) {
					let bl = [];
					for (let j = 0; j < lastBlockSize; j++) {
						bl.push(self.upStatis.dv.getUint8(self.upStatis.blockNum * self.res0.blockSize + j));
					}
					self.uploadData(bl, self.upStatis.curBlock, function(suc) {
						if (suc) {
							self.closeDrawer();
							//self.$Message.success("Success upload "+self.res0.name);
							self.cb({
								code: io.UP_FINISH
							}) //上传完成
						} else {
							//self.$Message.error("Fail upload "+self.res0.name);
							self.errMsg = "Fail upload " + self.res0.name;
							self.cb({
								code: io.UP_ERROR,
								msg: self.errMsg
							}) //报错
						}
					});
				} else {
					self.cb({
						code: io.UP_FINISH
					}) //上传完成
					self.resetUploadStatis();
				}
	
			}
		}
	
		getSizeVal(size) {
			let v = '';
			if (size < 1024) {
				v = size + 'B';
			} else if (size < 1024 * 1024) {
				v = this.toFixed(size / 1024, 2) + 'KB';
			} else if (size < 1024 * 1024 * 1024) {
				v = this.toFixed(size / (1024 * 1024), 2) + 'MB';
			} else {
				v = this.toFixed(size / (1024 * 1024 * 1024), 2) + 'GB';
			}
			return v;
		}
	
		getFinishSize(blockSize, curBlock) {
			let s = blockSize * (curBlock + 1);
			return this.getSizeVal(s);
		}
	
		getProgressVal(blockSize, curBlock, totalSize) {
			let s = blockSize * (curBlock + 1);
			return this.toFixed((s / totalSize) * 100, 0);
		}
	
		toFixed(val, num) {
			if (val) {
				return parseFloat(val).toFixed(num);
			} else {
				return ''
			}
		}
	
		getCostTime(startTime) {
			let v = '';
			let c = new Date().getTime() - startTime;
			if (c < 1000) {
				v = c + 'MS';
			} else if (c < 1000 * 60) {
				v = this.toFixed(c / 1000, 2) + 'S';
			} else if (c < 1000 * 60 * 60) {
				c = c / 1000;
				v = this.toFixed(c / 60, 2) + 'M ' + (c % 60) + 'S'
			} else {
				c = c / 1000;
				let h = c / (60 * 60);
	
				c = c % (60 * 60);
				let m = c / 60;
	
				let s = c % (60);
	
				v = this.toFixed(h, 0) + 'H ' + this.toFixed(m, 0) + 'M ' + this.toFixed(s, 0) + 'S'
			}
			return v;
		}
	
		getSpeedVal(blockSize, curBlock, startTime) {
			let s = blockSize * (curBlock + 1);
			let c = (new Date().getTime() - startTime) / 1000;
	
			if (c <= 0) {
				return '*';
			} else {
				let sp = s / c;
				return this.getSizeVal(sp) + '/M';
			}
		}
	
		resetUploadStatis() {
			this.upStatis = {
				finishSize: '',
				costTime: '',
				uploadSpeed: '',
				progressVal: 0,
	
				onUpload: false,
				totalLen: 0,
				blockNum: 0,
				dv: null,
				curBlock: 0,
				startTime: 0,
			}
			// this.refresh();
		}
}

export default {
	Uploader,
	
    __actreq :  function(method,args){
        return rpc.creq(this.sn,this.ns,this.v,method,args)
	},

  getResourceList: function (qry, pageSize, curPage) {
      return rpc.callRpc(this.__actreq( 'getResourceList', [qry, pageSize, curPage]))
  },

  addResource: function (name, size) {
      return rpc.callRpc(this.__actreq( 'addResource', [name, size]))
  },

  updateResource: function (res, updateFile) {
      return rpc.callRpc(this.__actreq( 'updateResource', [res, updateFile]))
  },

  addResourceData: function (resId, data, blockNum) {
    let req = {};

    req.serviceName = this.sn;
    req.namespace = this.ns;
    req.version = this.v;

    req.method = 'addResourceData';
    req.args = [resId, data, blockNum];
    return rpc.callRpc(req, rpc.Constants.PROTOCOL_BIN, rpc.Constants.PROTOCOL_JSON);
  },

  deleteResource: function (name) {
      return rpc.callRpc(this.__actreq( 'deleteResource', [name]))
  },

  queryDict: function () {
      return rpc.callRpc(this.__actreq( 'queryDict', []))
  },

  waitingResList: function (resId) {
      return rpc.callRpc(this.__actreq( 'waitingResList', [resId]))
  },

  dependencyList: function (resId) {
      return rpc.callRpc(this.__actreq( 'dependencyList', [resId]))
  },

  parseRemoteClass(resId){
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'parseRemoteClazz', [resId]);
  },

  sn:'cn.jmicro.choreography.api.IResourceResponsitoryJMSrv',
    ns: cons.NS_RESPOSITORY,
    v: '0.0.1'
}
