// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Keva',
  tagline: 'Fully open source low latency in-memory key-value database, used as Redis replacement',
  url: 'https://keva.dev',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/keva.jpg',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'keva-dev', // Usually your GitHub org/user name.
  projectName: 'keva', // Usually your repo name.

  // Even if you don't use internalization, you can use this field to set useful
  // metadata like html lang. For example, if your site is Chinese, you may want
  // to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/keva-dev/keva/tree/master/website/',
        },
        blog: {
          showReadingTime: true,
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      colorMode: {
        defaultMode: 'dark',
        disableSwitch: false,
        respectPrefersColorScheme: true,
      },
      navbar: {
        title: 'Keva',
        logo: {
          alt: 'Keva Logo',
          src: 'img/keva.jpg',
        },
        items: [
          {
            type: 'doc',
            docId: 'intro',
            position: 'left',
            label: 'Documentation',
          },
          {to: '/blog', label: 'Blog', position: 'left'},
          {
            href: 'https://github.com/keva-dev/keva',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'light',
        links: [
          {
            title: 'Product',
            items: [
              {
                label: 'Cloud',
                href: 'https://cloud.keva.dev/',
              },
              {
                label: 'Enterprise',
                href: 'https://cloud.keva.dev/',
              },
            ],
          },
          {
            title: 'Developer',
            items: [
              {
                label: 'Developer Guide',
                to: '/docs/developer-guide',
              },
              {
                label: 'Discussion',
                href: 'https://github.com/keva-dev/keva/discussions',
              },
              {
                label: 'Join Slack',
                href: 'https://join.slack.com/t/kevadev/shared_invite/zt-103vkwyki-pwum_qmcJgaOq6FIy3k2GQ',
              },
            ],
          },
          {
            title: 'Keva OSS',
            items: [
              {
                label: 'Keva IoC',
                href: 'https://github.com/keva-dev/keva-ioc',
              },
              {
                label: 'Keva Web',
                href: 'https://github.com/keva-dev/keva-web',
              },
              {
                label: 'Keva Reactif',
                href: 'https://github.com/keva-dev/reactif',
              },
            ],
          },
          {
            title: 'Team',
            items: [
              {
                label: 'About Us',
                href: 'https://github.com/keva-dev/keva',
              },
              {
                label: 'Grokking',
                href: 'https://www.grokking.org/',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} Keva team`,
      },
      prism: {
        additionalLanguages: ['java'],
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
      },
      image: 'img/preview.png',
    }),
  plugins: ['docusaurus-plugin-sass'],
};

module.exports = config;
