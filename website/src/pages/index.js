import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import HomepageFeatures from '@site/src/components/HomepageFeatures';

import styles from './index.module.scss';

import CommandlineSvg from '@site/static/img/commandline.svg';
import DockerSvg from '@site/static/img/docker.svg';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <header className={clsx('hero')}>
      <div className={styles.container}>
        <div className={styles.leftSide}>
            <h1 className="hero__title">{siteConfig.title}DB</h1>
            <p className="hero__subtitle">{siteConfig.tagline}</p>
            <div className={styles.buttons}>
                <Link className="button button--secondary button--lg" to="/docs/intro">
                  Documentation
                </Link>
                <Link className="button button--primary button--lg" href="https://cloud.keva.dev">
                  Try Cloud!
                </Link>
            </div>
        </div>
        <div className={styles.rightSide}>
          <CommandlineSvg />
          <div className={styles.commandBox}>
            <div>docker pull kevadev/keva-server</div>
            <div>docker run -p 6379:6379 kevadev/keva-server</div>
            <DockerSvg className={styles.dockerIcon}/>
          </div>
        </div>
      </div>
    </header>
  );
}

export default function Home() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      description="Fully open source low latency in-memory key-value database, used as Redis replacement">
      <HomepageHeader />
      <main>
        <HomepageFeatures />
      </main>
      <div className={styles.grokking}>
        <h3>Coordinated by</h3>
        <div><img src="https://i.imgur.com/5k8qMpf.png" alt="Grokking Vietnam"/></div>
        <div><a href="https://www.grokking.org/" target="_blank" rel="noreferrer">Grokking Vietnam</a></div>
      </div>
    </Layout>
  );
}
