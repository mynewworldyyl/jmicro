module.exports = {
  /*useEslint:false,*/
  presets: [
	 //['env',{'module':false}],
    '@vue/cli-plugin-babel/preset',
	//'@babel/preset-env',
	
  ],
  
    "plugins": [
      [
        "component",
        {
          "libraryName": "element-ui",
          "styleLibraryName": "theme-chalk"
        }
      ]
    ]
}
