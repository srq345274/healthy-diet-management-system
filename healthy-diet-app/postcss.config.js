const path = require('path')

module.exports = {
  parser: 'postcss-comment',
  plugins: [
    require('postcss-import')({
      resolve(id) {
        if (id.startsWith('~@/')) return path.resolve(process.env.UNI_INPUT_DIR, id.slice(3))
        if (id.startsWith('@/')) return path.resolve(process.env.UNI_INPUT_DIR, id.slice(2))
        if (id.startsWith('/') && !id.startsWith('//')) return path.resolve(process.env.UNI_INPUT_DIR, id.slice(1))
        return id
      }
    }),
    require('autoprefixer')({
      remove: process.env.UNI_PLATFORM !== 'h5'
    }),
    require('@dcloudio/vue-cli-plugin-uni/packages/postcss')
  ]
}