# Timezone data compiler

This is a port of the `zic` compiler:
<https://github.com/eggert/tz/commit/e28a7a4a1a5f497baae27ee63603efa88537607a>.
Its purpose is to transform plain text tables with timezone data
(such as <https://github.com/eggert/tz/blob/e29969d3f327f129ec4aebaa9c7aee96c46f8e3b/southamerica>)
to binary files that allow efficiently querying them.

## Scope

The port is purpose-built for `kotlinx-datetime-zoneinfo` and is expected to
evolve in the future. The support for TZif files is likely to stay, so that we
are able to easily verify the compatibility with the `zic` input data by
comparing their outputs; in addition to that, we should expect to implement
a different format in the future.

## Coding style

When introducing any changes to the tzdata compiler, keep future changes to
the upstream `zic` in mind. Broadly changing the structure of our code may
make it difficult to determine where to recreate any given upstream edit.
If our codebase ever becomes too dissimilar to the original `zic`, we should
consider introducing an index of the form
`<function in zic.c> -> <place in our codebase>`.

Because this is a tool for our internal usage, we do not hold this port to the
quality standards of the rest of `kotlinx-datetime`.

