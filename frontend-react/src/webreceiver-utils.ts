const groupToOrg = ( group: String ) => { return group.slice(2).replace('_','-')}

export { groupToOrg }