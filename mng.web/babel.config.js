module.exports = {
  /*useEslint:false,*/
  presets: [
    '@vue/cli-plugin-babel/preset',
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
