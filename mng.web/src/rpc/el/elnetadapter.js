// function getError(action, option, xhr) {
//   let msg;
//   if (xhr.response) {
//     msg = `${xhr.response.error || xhr.response}`;
//   } else if (xhr.responseText) {
//     msg = `${xhr.responseText}`;
//   } else {
//     msg = `fail to post ${action} ${xhr.status}`;
//   }

//   const err = new Error(msg);
//   err.status = xhr.status;
//   err.method = 'post';
//   err.url = action;
//   return err;
// }

// function getBody(xhr) {
//   const text = xhr.responseText || xhr.response;
//   if (!text) {
//     return text;
//   }

//   try {
//     return JSON.parse(text);
//   } catch (e) {
//     return text;
//   }
// }

// export default function upload(option) {
//   if (typeof XMLHttpRequest === 'undefined') {
//     return;
//   }

//   const xhr = new XMLHttpRequest();
//   const action = option.action;

//   if (xhr.upload) {
//     xhr.upload.onprogress = function progress(e) {
//       if (e.total > 0) {
//         e.percent = e.loaded / e.total * 100;
//       }
//       option.onProgress(e);
//     };
//   }

//   const formData = new FormData();

//   if (option.data) {
//     Object.keys(option.data).forEach(key => {
//       formData.append(key, option.data[key]);
//     });
//   }

//   formData.append(option.filename, option.file, option.file.name);

//   xhr.onerror = function error(e) {
//     option.onError(e);
//   };

//   xhr.onload = function onload() {
//     if (xhr.status < 200 || xhr.status >= 300) {
//       return option.onError(getError(action, option, xhr));
//     }

//     option.onSuccess(getBody(xhr));
//   };

//   xhr.open('post', action, true);

//   if (option.withCredentials && 'withCredentials' in xhr) {
//     xhr.withCredentials = true;
//   }

//   const headers = option.headers || {};

//   for (let item in headers) {
//     if (headers.hasOwnProperty(item) && headers[item] !== null) {
//       xhr.setRequestHeader(item, headers[item]);
//     }
//   }
//   xhr.send(formData);
//   return xhr;
// }

/**
 const options = {
   headers: this.headers,
   withCredentials: this.withCredentials,
   file: rawFile,
   data: this.data,
   filename: this.name,
   action: this.action,
   onProgress: e => {
     this.onProgress(e, rawFile);
   },
   onSuccess: res => {
     this.onSuccess(res, rawFile);
     delete this.reqs[uid];
   },
   onError: err => {
     this.onError(err, rawFile);
     delete this.reqs[uid];
   }
 };
 * @param {Object} option
 */
import io from '../io.js';
export default function upload(option) {
    let uploader = new io.Uploader()
    console.log('upload file: ',option)
    let file = option.file
    let res = option

   uploader.uploadFileH5(file, option, (rst) => {
      if(rst.code == io.UP_FINISH) {
        //self.fileId = res.id
        //self.uping = false
        //self.$emit("fileid", {fileId: self.fileId,code:io.UP_FINISH})
        option.onSuccess({fileId: res.id,data:{url:res.id}, code:io.UP_FINISH});
      }else if (rst.code == io.UP_RES) {
        console.log("io.UP_RES", res)
        res = rst.res
      }else if (rst.code == io.UP_ERROR) {
        console.log(rst)
        option.onError(rst);
      }else if (rst.code == io.UP_DATA) {
        //self.data = rst.data
      }else if (rst.code == io.UP_PROGRESS) {
        option.onProgress({percent:uploader.upStatis.progressVal});
      }
    })

}
