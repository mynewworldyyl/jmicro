const CompressionWebpackPlugin = require("compression-webpack-plugin");
//const TerserPlugin = require('terser-webpack-plugin')
const UglifyJs = require('uglifyjs-webpack-plugin')

//process.env.NODE_ENV='production';

const IS_PROD = ["production", "prod"].includes(process.env.NODE_ENV);
const productionGzipExtensions = /\.(js|css|json|txt|html|ico|svg)(\?.*)?$/i;

module.exports = {
    /*useEslint:false,*/
	devServer:{
		port:8081
	},
	
    publicPath:'/jmng/',//本地发布
	//publicPath:'/', //生产发布
    configureWebpack: config => {
        const plugins = [];
        if (IS_PROD) {
            plugins.push(
                new CompressionWebpackPlugin({
                    filename: "[path].gz[query]",
                    algorithm: "gzip",
                    test: productionGzipExtensions,
                    threshold: 10240,
                    minRatio: 0.8
                })
            );

            /*plugins.push(
                new TerserPlugin({
                    terserOptions: {
                        ecma: undefined,
                        warnings: false,
                        parse: {},
                        compress: {
                            drop_console: true,
                            drop_debugger: false,
                            pure_funcs: ['console.log'] // 移除console
                        }
                    }
                })
            );*/

           /*plugins.push(new UglifyJs({
               uglifyOptions: {
                   output: { // 删除注释
                       comments: false
                   },
                   //生产环境自动删除console
                   compress: {
                       //warnings: false, // 若打包错误，则注释这行
                       drop_debugger: true,  //清除 debugger 语句
                       drop_console: true,   //清除console语句
                       pure_funcs: ['console.log']
                   }
               },
               sourceMap: false,
               parallel: true
           }));*/

        }
        config.plugins = [...config.plugins, ...plugins];
    }
};