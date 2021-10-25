
/*
    This will need to be run once in each environment, but is safe to re-run if needed.
    Backfills all customerStatus values with correct status as of 9/20/2021.  It will create the missing property of the object if not present, and then insert the value specified.
 */

-- set all customerStatus to default 'inactive'
update setting
set values = jsonb_set(values, '{customerStatus}', '"inactive"', true)
where type in ('SENDER','RECEIVER');

-- set active senders and receivers to 'active'
update setting
set values = jsonb_set(values, '{customerStatus}', '"active"', true)
where type in ('SENDER','RECEIVER')
  and  values ->> 'organizationName' in ('simple_report','waters','safehealth','cue','inbios','imagemover','anavasidx','strac',
    'tpca','careevolution','primary','johns_hopkins','reddyfmc-la','guc-la','cuc-al',
    'az-phd','pima-az-phd','ca-scc-phd','co-phd','tx-phd','fl-phd','gu-doh','vt-doh',
    'nd-doh','la-doh','oh-doh','nm-doh','mt-doh','tx-doh','nj-doh','mn-doh','ms-doh',
    'ca-dph','ma-phd','al-phd','pa-phd','pa-chester-phd','pa-montgomery-phd','pa-philadelphia-phd',
    'md-doh','md-phd','hhsprotect','ignore','il-phd','wa-phd','wy-phd','ak-phd','nh-dphs','all-in-one-health-ca','or-phd');

-- set testing senders and receivers to 'testing'
update setting
set values = jsonb_set(values, '{customerStatus}', '"testing"', true)
where type in ('SENDER','RECEIVER')
  and  values ->> 'organizationName' in ('wi-dph','tn-doh','de-dph','ct-phd','ny-phd','fl-hillsborough-phd');

