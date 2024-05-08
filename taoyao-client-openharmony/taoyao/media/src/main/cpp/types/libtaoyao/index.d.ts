export const init: (
  json   : string,
  push   : (signal: string, body: string, id: number) => void,
  request: (signal: string, body: string, id: number) => Promise<string>
) => number;
export const shutdown  : (json: string) => number;
export const roomClose : (json: string) => number;
export const roomEnter : (json: string) => number;
export const roomExpel : (json: string) => number;
export const roomInvite: (json: string) => number;
export const roomLeave : (json: string) => number;
export const roomClientList     : (json: string) => number;
export const mediaConsume       : (json: string) => number;
export const mediaConsumerClose : (json: string) => number;
export const mediaConsumerPause : (json: string) => number;
export const mediaConsumerResume: (json: string) => number;
export const mediaProducerClose : (json: string) => number;
export const mediaProducerPause : (json: string) => number;
export const mediaProducerResume: (json: string) => number;
