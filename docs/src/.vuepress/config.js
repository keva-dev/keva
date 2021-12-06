module.exports = {
  /**
   * Ref：https://v1.vuepress.vuejs.org/config/#title
   */
  title: 'Keva',
  /**
   * Ref：https://v1.vuepress.vuejs.org/config/#description
   */
  description: 'Low-latency in-memory key-value store, used as a database or cache',

  /**
   * Extra tags to be injected to the page HTML `<head>`
   *
   * ref：https://v1.vuepress.vuejs.org/config/#head
   */
  head: [
    ['meta', { name: 'theme-color', content: '#000000' }],
    ['meta', { name: 'apple-mobile-web-app-capable', content: 'yes' }],
    ['meta', { name: 'apple-mobile-web-app-status-bar-style', content: 'black' }],
    ['meta', { property: 'og:image', content: "/preview.png" }],
    ['link', { rel: "apple-touch-icon", sizes: "180x180", href: "/keva.png"}],
    ['link', { rel: "icon", type: "image/png", sizes: "32x32", href: "/keva.png"}],
    ['link', { rel: "icon", type: "image/png", sizes: "16x16", href: "/keva.png"}],
  ],

  /**
   * Theme configuration, here is the default theme configuration for VuePress.
   *
   * ref：https://v1.vuepress.vuejs.org/theme/default-theme-config.html
   */
  themeConfig: {
    logo: 'https://i.imgur.com/z0c9bV7.png',
    repo: 'https://github.com/keva-dev/keva',
    editLinks: false,
    docsDir: '',
    editLinkText: '',
    lastUpdated: false,
    nav: [
      {
        text: 'Documentations',
        link: '/guide/',
      },
      {
        text: 'Blogs',
        link: 'https://tuhuynh.com/tags/keva',
      },
      {
        text: 'Team',
        link: '/team/',
      },
      {
        text: 'Keva IoC',
        link: 'https://github.com/keva-dev/keva-ioc',
      },
      {
        text: 'Keva Web',
        link: 'https://github.com/keva-dev/keva-web',
      },
    ],
    sidebar: {
      '/guide/': [
        {
          title: 'Overview',
          collapsable: false,
          children: [
            '',
            'overview/install',
            'overview/commands',
            'overview/security',
            'overview/replication',
            'overview/kql',
          ]
        },
        {
          title: 'Insight',
          collapsable: false,
          children: [
            'insight/design',
            'insight/benchmark'
          ]
        },
        'roadmap',
        'developer-guide'
      ],
    }
  },

  /**
   * Apply plugins，ref：https://v1.vuepress.vuejs.org/zh/plugin/
   */
  plugins: [
    '@vuepress/plugin-back-to-top',
    '@vuepress/plugin-medium-zoom',
  ]
}
