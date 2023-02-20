# selkie

_From wikipedia_

**In Celtic and Norse mythology, selkies or selkie folk meaning 'seal folk' are mythological beings capable of therianthropy, changing from seal to human form by shedding their skin.**


Selkie is a shapeshifter rest api that, using `router functions` can create an api **(Read Only)** from configuration.

##  Configuration

It uses spring configuration properties (ie:`application.yaml`)

### Schema
```yaml
type: object
properties:
  data-bin:
    type: object
    properties:
      seed:
        type: object
        additionalProperties: true
      meta:
        type: object
        additionalProperties: true
      embedded:
        type: array
        items:
          type: object
          properties:
            service: 
              type: string
            request:
              type: object
              properties:
                query:
                  type: object
                  properties:
                    name:
                      type: string
                    field:
                      type: string    

```

### Example

```yaml

data-bin:
  resource:
    name: countries
    seed:
      AC:
        description: A Country
      NC:
        description: No Country
      ANC:
        description: Another country with meta
    meta:
      ANC:
        coin:
          - EUR
          - BTC
        language:
          - ESP
          - PT
    embedded:
    - name: coins
      service: coins
      request:
        query:
        - name: country
          field: id
    - name: languages
      service: languages
      request:
        query:
        - name: country
          field: id

```
