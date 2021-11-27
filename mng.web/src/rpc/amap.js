import AMapLoader from '@amap/amap-jsapi-loader';
//import { Geolocation } from '@ionic-native/geolocation';

let AMap;

 let getAMap = function () {
  return new Promise((reso,reje)=>{
    if(AMap) {
      reso(AMap)
    } else {
      AMapLoader.load({
          "key": "ddd292c88aa1bad9c04891a47724f40a", //申请好的Web端开发者Key，首次调用 load 时必填
          "version": "1.4.15", //指定要加载的 JSAPI 的版本，缺省时默认为 1.4.15
          "plugins": ["AMap.Geocoder","AMap.Geolocation","AMap.DistrictLayer"], //需要使用的的插件列表，如比例尺'AMap.Scale'等
          "AMapUI": { //是否加载 AMapUI，缺省不加载
              "version": '1.1', //AMapUI 缺省 1.1
              "plugins": [], //需要加载的 AMapUI ui插件
          },
          "Loca": { //是否加载 Loca， 缺省不加载
              "version": '1.3.2' //Loca 版本，缺省 1.3.2
          },
      }).then((AMap0) => {
          console.log('Load amap successfully!')
          AMap = AMap0;
          reso(AMap)
      }).catch(e => {
          console.log(e);
          reje(e)
      })
    }
  });
}

let convertFrom = function(co,type) {
  //var gps = [116.3, 39.9
  return new Promise((reso,reje)=>{
    getAMap()
    .then(AMap=>{
      AMap.convertFrom(co, type, function (status, result) {
        if (result.info === 'ok') {
          var lnglats = result.locations; // Array.<LngLat>
          reso(lnglats)
        }else{
          reje(result.info)
        }
      });
    })
    .catch(err=>{
      reje(err)
    })
  })
}

let getPosition0 = function(){
  return new Promise((reso,reje)=>{
    const suc = position=>{
      // resp.coords.latitude
      // resp.coords.longitude
      var coords = position.coords;
      reso(coords);
    }

    const fail = error=>{
      console.log('Error getting location', error);
      reje(error);
    };

    if(window.navigator.geolocation) {
      window.navigator.geolocation.getCurrentPosition(suc,fail,{
        //指示浏览器获取高精度的位置，默认为false
        enableHighAccuracy:true,
        //指定获取地理位置的超时时间，默认不限时，单位为毫秒
        timeout: 1000000,
        //最长有效期，在重复获取地理位置时，此参数指定多久再次获取位置。
        maximumAge: 1000000
      })
    }
  }
)
}

let getPosition = function(){
  return new Promise((reso,reje)=>{
    const geol = new AMap.Geolocation({
        enableHighAccuracy: true,//是否使用高精度定位，默认:true
        timeout: 10000,          //超过10秒后停止定位，默认：无穷大
        maximumAge: 0,           //定位结果缓存0毫秒，默认：0
        convert: true,           //自动偏移坐标，偏移后的坐标为高德坐标，默认：true
        showButton: false,        //显示定位按钮，默认：true
        buttonPosition: 'LB',    //定位按钮停靠位置，默认：'LB'，左下角
        buttonOffset: new AMap.Pixel(10, 20),//定位按钮与设置的停靠位置的偏移量，默认：Pixel(10, 20)
        showMarker: false,        //定位成功后在定位到的位置显示点标记，默认：true
        showCircle: false,        //定位成功后用圆圈表示定位精度范围，默认：true
        panToLocation: false,     //定位成功后将定位到的位置作为地图中心点，默认：true
        zoomToAccuracy:false      //定位成功后调整地图视野范围使定位位置及精度范围视野内可见，默认：false
    });
    //mapObj.addControl(geolocation);
    geol.getCurrentPosition((status,result)=>{
      //console.log(status,result)
      if("SUCCESS" == result.info) {
          reso(result)
      }else {
          reje(result.message)
      }
    });
    //AMap.event.addListener(geol, 'complete', suc);//返回定位信息
    //AMap.event.addListener(geol, 'error', fail);      //返回定位出错信息
  }
)

}


export {getAMap}
export {convertFrom}
export {getPosition,getPosition0}
