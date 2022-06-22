import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: 'Redis Drop-in Alternative',
    Svg: require('@site/static/img/db.svg').default,
    description: (
      <>
          Front-end tools and Redis clients will work with Keva, it is a drop in alternative to Redis
      </>
    ),
  },
  {
    title: 'High Throughput',
    Svg: require('@site/static/img/up.svg').default,
    description: (
      <>
        Keva comes with multithreaded engine which will maximize the use of your system's resources
      </>
    ),
  },
  {
    title: 'High Availability',
    Svg: require('@site/static/img/cross.svg').default,
    description: (
      <>
          Simplify your high availability setup with master-replica nodes and sharded cluster mode
      </>
    ),
  },
];

function Feature({Svg, title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <Svg className={styles.featureSvg} role="img" />
      </div>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
