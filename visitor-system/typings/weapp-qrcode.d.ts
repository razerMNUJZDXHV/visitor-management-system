// miniprogram/typings/weapp-qrcode.d.ts
declare module 'weapp-qrcode' {
  interface QRCodeOptions {
    text: string;
    width: number;
    height: number;
    colorDark?: string;
    colorLight?: string;
    correctLevel?: number;
  }

  class QRCode {
    static CorrectLevel: {
      L: number;
      M: number;
      Q: number;
      H: number;
    };

    constructor(canvasId: string, options: QRCodeOptions);
  }

  export default QRCode;
}