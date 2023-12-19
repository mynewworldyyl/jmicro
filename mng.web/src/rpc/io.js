import rpc from "./rpcbase.js"
import utils from "./utils.js"
import {
	Constants
} from './message';

const addfile_code = 1476988847
const adddata_code = -1011802857

let io = {
	SUCCESS:0,
	RT_BUFF: 1,
	RT_URL: 2,
	RT_STRING: 3,
	RT_BYTE: 4,

	UP_DATA: 1,
	UP_FINISH: 2,
	UP_PROGRESS: 3,
	UP_ERROR: 4,
	UP_RES: 5,

	newUploader(srvCode) {
		return new Uploader(srvCode ? !srvCode : srv_code)
	},

	getFileInfo(path) {
		return new Promise((reso,reje)=>{
			plus.io.resolveLocalFileSystemURL(path, (entry)=>{
				entry.file((file)=> {
					reso({code:this.SUCCESS, res: file})
				});
			}, (e)=>{
				reso({
					msg: e.message,
					code: this.UP_ERROR
				})
			});
		})
	},

	writeBase64AsBin(base64Str,fileName) {
		console.log("writeBase64AsBin: " + fileName)
		if(utils.isUni()) {
			// #ifdef APP
			console.log("APP: "+fileName)
			if('android' == uni.$u.os()) {
				console.log("android: "+fileName)
				return this._saveFile2AndroidSys(base64Str,fileName)
			}else if('ios' == uni.$u.os()){
				return this._saveFile2IosSys(base64Str,fileName)
			}
			//#endif

			// #ifdef H5
			throw 'Not support H5 yet'
			return new Promise((reso,reje)=>{

			})
			//#endif

			// #ifdef MP-WEIXIN
			throw 'Not support weixin yet'
			return new Promise((reso,reje)=>{

			})
			//#endif
		} else {
				throw 'Not support non UniApp yet'
		}
	},

	existFile(fileName) {
		let self = this
		return new Promise((reso,reje)=>{
			plus.io.requestFileSystem(plus.io.PRIVATE_DOC,( fs )=> {
				fs.root.getFile(fileName, {create:false},
				(entry)=>{
					console.log("existFile: ", entry)
					let ex = entry && entry.fullPath
					reso({code:self.SUCCESS, data:ex})
				})
			}, (e)=> {
				console.log("existFile: ",e)
				reso({code:self.UP_ERROR, msg:JSON.stringify(e)})
			} );
		})
	},

	removeFile(fileName) {
		let self = this
		return new Promise((reso,reje)=>{
			plus.io.requestFileSystem(plus.io.PRIVATE_DOC,( fs )=> {
				fs.root.getFile(fileName, {create:false},
				(entry)=>{
					console.log("delFile: ", entry)
					let ex = entry && entry.fullPath
					if(ex) {
						entry.remove((r)=>{
							console.log(r)
						},(e)=>{
							console.log(e)
						})
					}
					reso({code:self.SUCCESS, data:ex})
				})
			}, (e)=> {
				console.log("delFile: ",e)
				reso({code:self.UP_ERROR, msg:JSON.stringify(e)})
			} );
		})
	},

	_saveFile2AndroidSys(base64Str,fileName){
		console.log("_saveFile2AndroidSys: "+fileName)
		let self = this
		return new Promise((reso,reje)=>{
			plus.io.requestFileSystem(plus.io.PRIVATE_DOC,( fs )=> {
					fs.root.getFile(fileName, {create:true},
					(entry)=>{
						console.log("write local path: " + entry.fullPath)
						let out = null
						try{
							let FileOutputStream = plus.android.importClass("java.io.FileOutputStream")
							let b64Util = plus.android.importClass("android.util.Base64")
							/*
							let pre = base64Str.substring(0,60)
							let idx = pre.indexOf(";base64,") //只在前60个字符查找
							if(idx > 0) {
								//去除前缀  "data:image/jpeg;base64,"
								base64Str = base64Str.substr(idx+8)
							}
							*/
							//console.log(base64Str.substring(0,100))
							//转为byte数组
							//let bytes = utils.base642ByteArr(base64Str)

							let bytes = b64Util.decode(base64Str,b64Util.DEFAULT)
							console.log(bytes)
							let out = new FileOutputStream(entry.fullPath)
							out.write(bytes,0,bytes.length)
							reso({code:self.SUCCESS, data:entry.fullPath})
						} catch(e) {
							console.log("write error: ",e)
							reso({code:self.UP_ERROR, msg:JSON.stringify(e)})
						}finally{
							if(out != null) {
								try{
									out.close()
								}catch(e) {
									console.log(e)
								}
							}
						}
					})
			}, (e)=> {
				console.log("requestFileSystem: ",e)
				reso({code:self.UP_ERROR, msg:JSON.stringify(e)})
			} );
		})
	},

	/**
	 * 读取文件内容
	 * @param {Object} f 在uniapp平台，f={path:'文件路径'}，在H5平台，为FileInfo实例，可直接用于FileReader读取内容
	 * @param {Object}
	 * readDataType self.RT_BYTE 或self.RT_BUFF返回 ArrayBuffer
	 * readDataType RT_URL 返回 Base64 URL
	 * readDataType RT_String 返回 本文字符串
	 */
	getFileContent(f, readDataType) {
		let self = this
		return new Promise(function(reso, reje) {
			if (!f) {
				reje("file cannot be null")
				return
			}

			if (!readDataType) {
				readDataType = self.RT_URL //默认读BASE64 URL
			}

			console.log("plus.io.FileReader")
			//uniapp平台
			//https://uniapp.dcloud.io/api/media/file.html#choosefile
			//file uni.chooseFile(OBJECT) 的返回值
			self.getFileInfo(f.path)
			.then(rst=>{
				console.log('entry.file: ',rst)
				if(rst.code != self.SUCCESS) {
					reso(rst)
					return
				}

				var reader = new plus.io.FileReader();
				reader.onload = function(evt) {
					//console.log('onload: ',evt)
					let d = evt.target.result
					//BASE64_IMAGE_PREFIX = "data:image/jpeg;base64,";
					//console.log(d.substr(0,300))
					if(self.RT_BYTE == readDataType || self.RT_BUFF == readDataType) {
						let idx = d.indexOf(";base64,")
						let prefix = d.substr(0,idx+8)
						d = d.substr(idx+8)
						//console.log(d.substr(0,300))
						//转为byte数组
						let bd = utils.base642ByteArr(d)
						//返ArrayBuffer
						reso({data: bd, code: self.SUCCESS,b64Prefix:prefix})
					}else{
						//返回self.RT_URL 或 String
						reso({data: d, code: self.SUCCESS})
					}
				}

				reader.onerror = (evt) => {
					console.log("onerror",evt)
					reso({
						data: evt.message,
						code: self.UP_ERROR
					})
				}

				switch (readDataType) {
					case self.RT_STRING:
						reader.readAsText(rst.res, 'utf-8');
						break;
					case self.RT_URL:
					case self.RT_BYTE:
					case self.RT_BUFF:
						//Base64格式数据，数据格式如下
						//data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD
						reader.readAsDataURL(rst.res, 'utf-8');
						break;
				}
			})

		})
	},

  getFileContentH5(f, readDataType) {
  	let self = this
  	return new Promise(function(reso, reje) {
  		if (!f) {
  			reje("file cannot be null")
  			return
  		}

  		if (!readDataType) {
  			readDataType = self.RT_URL //默认读BASE64 URL
  		}

  		console.log("H5 FileReader")
  		//H5平台
  		let reader = new FileReader();
  		//reader.readAsArrayBuffer(file);
  		//reader.onabort = () => {};
  		//当读取操作发生错误时调用
  		reader.onerror = (e) => {
  			console.log(e)
  			reje({
  				code: self.UP_ERROR,
  				msg: 'read file error'
  			});
  		};

  		//当读取操作成功完成时调用
  		reader.onload = (e) => {
  			reso({
  				data: e.target.result,
  				code: self.SUCCESS
  			});
  		};
  		//当读取操作完成时调用,不管是成功还是失败
  		reader.onloadend = (e) => {console.log(e)};

  		//当读取操作将要开始之前调用
  		//reader.onloadstart = () => {};

  		//在读取数据过程中周期性调用
  		//reader.onprogress = () => {};

  		switch (readDataType) {
  			case self.RT_STRING:
  				reader.readAsText(f);
  				break;
  			case self.RT_BYTE:
  				reader.readAsBinaryString(f);
  				break;
  			case self.RT_URL:
  				//Base64格式数据，数据格式如下
  				//data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD
  				reader.readAsDataURL(f);
  				break;
  			case self.RT_BUFF:
  				reader.readAsArrayBuffer(f);
  				break;
  		}
  	})
  },

}

class Downloader {

	/**
	 * @param {Object} srvCode 上传文件服务接口编码
	 */
	constructor() {
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
			startTime: 0
		}
	}

	fileInfo(fsId, fileName) {

	}

	download(fsId, fileName) {

	}

}



class Uploader {

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
			startTime: 0
		}
	}

	uploadArrayBuffer(buf, res, cb,H5) {
    if(!H5) {
      this._uploadArrayBufferNonH5(buf,res,cb)
    } else {
      this._uploadArrayBufferH5(buf,res,cb)
    }
	}

	/**
	 path: (...)
	 lastModified: 1569233132049
	 lastModifiedDate: Mon Sep 23 2019 18:05:32 GMT+0800 (中国标准时间) {}
	 name: "ddd.jpg"
	 size: 421322
	 type: "image/jpeg"
	 webkitRelativePath: ""
	 * @param {Object} buf
	 * @param {Object} res0
	 * @param {Object} cb
	 */
	_uploadArrayBufferH5(buf, res0, cb) {
		let res = res0.file
		this.cb = cb
		console.log('in uploadArrayBuffer: ', res)
		let ps = {
			type: res.type,
			name: res.name,
			size: res.size,
			lastModified: res.lastModified,
			tochar : res0.tochar
		}
		this.addFile(ps,buf)
	}

	/**
	 * @param {Object} arrayBuffer 二进制数组
	 * @param {Object} cb 上传结果通知回调
	 */
	 _uploadArrayBufferNonH5(buf, res, cb) {
		let self = this
		this.cb = cb
		console.log('in uploadArrayBuffer: ', res)

		let ps = {
			type: res.type,
			name: res.name,
			size: buf.byteLength ? buf.byteLength : buf.length,
			lastModified: res.lastModified,
			tochar : res.tochar
		}

		if (res.id && res.id > 0) {
			//rep.updateResource(self.res,true)
			if(!res.type) {
				//uniapp平台取得文件信息
				io.getFileInfo(res.path)
				.then(fi=>{
					ps = {
						type : fi.res.type,
						name : fi.res.name,
						size : buf.byteLength?buf.byteLength:buf.length,
						tochar : res.tochar,
						lastModified : fi.res.lastModifiedDate.getTime()
					}
					self.addFile(ps,buf)
				})
			} else {
				self.addFile(ps,buf)
			}
		} else {
			//rep.addResource(self.res)
			console.log(JSON.stringify(res))
			if(!res.type) {
				io.getFileInfo(res.path)
				.then(fi=>{
					//uniapp平台取得文件信息
					console.log("uploadArrayBuffer getFileInfo: ",fi)
					if(fi.code == io.SUCCESS) {
						ps = {
							type: fi.res.type,
							name: fi.res.name,
							size: buf.byteLength?buf.byteLength:buf.length,
							tochar : res.tochar,
							lastModified: fi.res.lastModifiedDate.getTime()
						}
						self.addFile(ps,buf)
					} else {
						this.cb(fi)
					}
				})
			} else {
				self.addFile(ps,buf)
			}
		}
	}

	addFile(ps,buf) {
		let self = this;
		console.log("addFile invokeByCode"+JSON.stringify(ps))
		rpc.invokeByCode(addfile_code, [ps])
			.then((resp) => {
				console.log("addFile:"+JSON.stringify(resp))
				if (resp.code != 0) {
					this.cb({
						code: io.UP_ERROR,
						msg: JSON.stringify(resp)
					})
					return;
				}
				//console.log("addFile",JSON.stringify(resp))
				this.cb({
					code: io.UP_RES,
					res: resp.data
				})
				console.log(buf)
				self.initProgressData(buf, resp.data)
				self.uploadCurBlock(buf, resp.data)
			}).catch((err) => {
				this.cb({
					code: io.UP_ERROR,
					msg: err
				})
			});
	}

	/**
	 * H5平台File实例内容如下：
	 path: "blob:http://localhost:8081/c5c3c866-cd98-460a-ad16-98e4970f162f"
	 lastModified: 1599829296004
	 lastModifiedDate: Fri Sep 11 2020 21:01:36 GMT+0800 (中国标准时间)
	 [[Prototype]]: Object
	 name: "33.jpg"
	 size: 43409
	 type: "image/jpeg"
	 webkitRelativePath: ""

	 uniapp平台File如下：
	 ), fullPath: "/storage/emulated/0/Android/data/io.dcloud.HBuilder/apps/HBuilder/doc/1647089071737.jpg"}
	 fullPath: "/storage/emulated/0/Android/data/io.dcloud.HBuilder/apps/HBuilder/doc/1647089071737.jpg"
	 lastModifiedDate: Sat Mar 12 2022 20:44:38 GMT+0800 (中国标准时间)
	 [[Prototype]]: Object
	 name: "1647089071737.jpg"
	 size: 5506655
	 type: "image/jpeg"
	 [[Prototype]]: Object

	 * @param {Object} file 要上传的文件
	 * @param {Object} cb 上传结果通知回调
	 */
	uploadFile(file, res, cb) {
		if(!file) {
			cb({
				code: io.UP_ERROR,
				msg: '文件不能为空'
			})
			return;
		}

		//H5平台，type文件实例，UniAPP平台，res.type为空
		let mt = io.RT_BUFF //res.type ? io.RT_BUFF : io.RT_URL

		let self = this
		io.getFileContent(file, mt).then((rst) => {
			console.log('getFileContent: ', rst)
			if (rst.code == io.SUCCESS) {
				if(!res.type && rst.type) {
					res.type = rst.type
				}
				console.log('invoke uploadArrayBuffer: ', rst)
				if(mt == io.RT_BUFF) {
					self.uploadArrayBuffer(rst.data, res, cb,false)
				} else {
					//通知BAse64数据
					cb({code:io.UP_DATA, data:rst.data})
					self.uploadStringFile(rst.data, res, cb)
				}

			} else {
				cb(rst)
			}
		}).catch(err => {
			console.log(err);
			if (err.code) {
				cb(err)
			} else {
				this.cb({
					code: io.UP_ERROR,
					msg: err
				})
			}
		});
	}

  uploadFileH5(file, res, cb) {
  	if(!file) {
  		cb({
  			code: io.UP_ERROR,
  			msg: '文件不能为空'
  		})
  		return;
  	}

  	//H5平台，type文件实例，UniAPP平台，res.type为空
  	let mt = io.RT_BUFF //res.type ? io.RT_BUFF : io.RT_URL

  	let self = this
  	io.getFileContentH5(file, mt).then((rst) => {
  		console.log('getFileContentH5: ', rst)
  		if(rst.code == io.SUCCESS) {
  			if(!res.type && rst.type) {
  				res.type = rst.type
  			}
  			console.log('invoke uploadArrayBuffer: ', rst)
  			if(mt == io.RT_BUFF) {
  				self.uploadArrayBuffer(rst.data, res, cb,true)
  			} else {
  				//通知BAse64数据
  				cb({code:io.UP_DATA, data:rst.data})
  				self.uploadStringFileH5(rst.data, res, cb)
  			}

  		} else {
  			cb(rst)
  		}
  	}).catch(err => {
  		console.log(err);
  		if (err.code) {
  			cb(err)
  		} else {
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
		let b = utils.byteArray2ArrayBuffer(ar)
		res.tochar = true
		this.uploadArrayBuffer(b, res, cb)
	}

  uploadStringFileH5(fileStr, res, cb) {
  	let ar = utils.toUTF8Array(fileStr)
  	let b = utils.byteArray2ArrayBuffer(ar)
  	res.tochar = true
  	this.uploadArrayBuffer(b, res, cb,true)
  }

	uploadData(data, blockNum, res, cb) {
		let self = this;
		//rep.addResourceData(this.res.id, data, blockNum)
		rpc.invokeByCode(adddata_code, [res.id, data, blockNum],Constants.PROTOCOL_BIN, Constants.PROTOCOL_JSON)
			.then((resp) => {
				//console.log(resp)
				if (resp.code == 0) {
					cb(true);
				} else {
					console.log(resp)
					cb(false);
				}
			})
			.catch((err) => {
				console.log(err)
				cb(false, err);
			});
	}

	initProgressData(buf, res) {
		console.log('initProgressData:' + JSON.stringify(res))
		let self = this;
		let totalLen = buf.byteLength ? buf.byteLength : buf.length; // buf.byteLength;
		self.upStatis.totalLen = totalLen;
		self.upStatis.totalSize = self.getSizeVal(totalLen);
		self.upStatis.onUpload = true;
		self.upStatis.blockNum = parseInt(totalLen / res.blockSize);

		if(buf.byteLength) {
			//ArrayBuffer
			self.upStatis.bytes = false
			self.upStatis.dv = new DataView(buf, 0, totalLen);
		}else {
			//字节数组
			self.upStatis.bytes = true
			self.upStatis.dv = buf; // new DataView(buf, 0, totalLen);
		}

		self.upStatis.curBlock = 0;
		self.upStatis.startTime = new Date().getTime();
	}

	uploadCurBlock(buf, res) {
		let self = this;
		console.log('uploadCurBlock curBlock:: ' + self.upStatis.curBlock)

		self.upStatis.finishSize = self.getFinishSize(res.blockSize, self.upStatis.curBlock);
		self.upStatis.costTime = self.getCostTime(self.upStatis.startTime);
		self.upStatis.progressVal = parseInt(self.getProgressVal(res.blockSize, self.upStatis.curBlock, self.upStatis.totalLen));
		self.upStatis.uploadSpeed = self.getSpeedVal(res.blockSize, self.upStatis.curBlock, self.upStatis.startTime);

		//console.log("uploadCurBlock self.upStatis.blockNum:",self.upStatis.blockNum)
		//console.log('uploadCurBlock:' + JSON.stringify(self.upStatis))

		if(self.upStatis.curBlock < self.upStatis.blockNum) {
			let bl = [];
			for (let j = 0; j < res.blockSize; j++) {
				if(self.upStatis.bytes) {
					bl.push(self.upStatis.dv[res.blockSize * self.upStatis.curBlock + j]);
				}else {
					bl.push(self.upStatis.dv.getUint8(res.blockSize * self.upStatis.curBlock + j));
				}
			}
			//console.log("uploadCurBlock num:"+self.upStatis.curBlock)
			self.uploadData(bl, self.upStatis.curBlock, res, (success, err) => {
				if (success) {
					self.upStatis.curBlock += 1;
					self.cb({code: io.UP_PROGRESS,statis: self.upStatis}) //更新进度
					self.uploadCurBlock(buf, res);
				} else {
					let errMsg = "Fail upload: " + res.name;
					console.log(err)
					console.log(errMsg)
					self.cb({code: io.UP_ERROR,msg: errMsg,err}) //报错
				}
			});
		} else if (self.upStatis.curBlock == self.upStatis.blockNum) {
			//最后一块
			let lastBlockSize = self.upStatis.totalLen % res.blockSize;
			if (lastBlockSize > 0) {
				let bl = [];
				for(let j = 0; j < lastBlockSize; j++) {
					if(self.upStatis.bytes) {
						bl.push(self.upStatis.dv[self.upStatis.blockNum * res.blockSize + j]);
					} else {
						bl.push(self.upStatis.dv.getUint8(self.upStatis.blockNum * res.blockSize + j));
					}
				}
				console.log("uploadCurBlock last block: ",self.upStatis.curBlock+1)
				self.uploadData(bl, self.upStatis.curBlock, res, function(suc,err) {
					if (suc) {
						//self.closeDrawer();
						//self.$Message.success("Success upload "+res.name);
						console.log("finish upload:",res)
						self.resetUploadStatis()
						self.cb({
							code: io.UP_FINISH
						}) //上传完成
					} else {
						//self.$Message.error("Fail upload "+res.name);
						let errMsg = "Fail upload " + res.name+", error: "+err;
						self.cb({
							code: io.UP_ERROR,
							msg: errMsg
						}) //报错
						console.log(err)
						console.log(errMsg)
					}
				});
			} else {
				console.log("finish11:",res)
				self.resetUploadStatis()
				self.cb({
					code: io.UP_FINISH
				}) //上传完成
			}

		}
	}

	getSizeVal(size) {
		return utils.getSizeVal(size)
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
		return utils.toFixed(val, num)
	}

	getCostTime(startTime) {
		return utils.getCostTime(new Date().getTime() - startTime)
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

io.Uploader = Uploader
io.Downloader = Downloader

export default io
