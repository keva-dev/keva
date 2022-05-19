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
        text: 'Try Keva Cloud',
        link: 'https://cloud.keva.dev',
      },
      {
        text: 'Documentations',
        link: '/guide/',
      },
      {
        text: 'Blogs',
        link: 'https://blog.keva.dev',
      },
      {
        text: 'Team',
        link: '/team/',
      },
      {
        text: 'Slack',
        link: 'https://join.slack.com/t/kevadev/shared_invite/zt-103vkwyki-pwum_qmcJgaOq6FIy3k2GQ',
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
            'overview/persistence',
            'overview/security',
            'overview/replication',
            'overview/embedded',
            'overview/kql',
          ]
        },
        {
          title: 'Insight',
          collapsable: false,
          children: [
            'insight/design',
            'insight/benchmark',
            'insight/release-notes',
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
  ],
  markdown: {
    extendMarkdown: md => {
      md.use(require('markdown-it-task-lists'))
    }
  }
}
