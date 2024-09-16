program ClearSubClass;

{$APPTYPE CONSOLE}

{$R *.res}

uses
  Winapi.Windows,
  System.Math,
  System.SysUtils,
  UnitVerySimpleXml in 'VerySimpleXml\UnitVerySimpleXml.pas',
  Xml.VerySimple in 'VerySimpleXml\Xml.VerySimple.pas';

var GpxFile: string;
    Xml: TXmlVerySimple;
    GpxNode: TXmlNode;
    FormatSettings: TFormatSettings;

const DirectRoutingClass = '000000000000FFFFFFFFFFFFFFFFFFFFFFFF';
      RouteSubClass = '0000';
      GpxNodename = 'gpx';
      RteNodename = 'rte';
      RteNameNodeName = 'name';
      RtePtNodename = 'rtept';
      RtePtNameNodeName = 'name';
      SubClassNodename = 'gpxx:Subclass';
      ExtensionsNodename = 'extensions';
      RoutePointExtensionsNodename = 'gpxx:RoutePointExtension';

function GetLocaleSetting: TFormatSettings;
begin
  // Get Windows settings, and modify decimal separator and negcurr
  Result := TFormatSettings.Create(GetThreadLocale);
  with Result do
  begin
    DecimalSeparator := '.'; // The decimal separator is a . PERIOD!
    NegCurrFormat := 11;
  end;
end;

procedure ProcessCoords(ANode: TXmlNode);
var Coord: Double;
    Attrib: TXmlAttribute;
begin
  for Attrib in ANode.AttributeList do
  begin
    if (Attrib.Name = 'lat') or
       (Attrib.Name = 'lon') then
    begin
      Coord := StrToFloat(Attrib.Value, FormatSettings);
      Coord := RoundTo(Coord, -6);
      Attrib.Value := FloatToStr(Coord, FormatSettings);
    end;
  end;
end;

function FindSubNodeValue(ANode: TXmlNode;
                          SubName: string): string;
var SubNode: TXmlNode;
begin
  Result := '';
  SubNode := ANode.Find(SubName);
  if (SubNode <> nil) then
    Result := SubNode.NodeValue;
end;

function ProcessSubClass(ANode: TXmlNode): boolean;
var SubClassNode: TXmlNode;
begin
  result := false;
  if (ANode = nil) then
    exit;
  SubClassNode := ANode.Find(SubClassNodename);
  if (SubClassNode <> nil) then
  begin
    if (ANode.NodeName = 'gpxx:rpt') then
    begin
    // Subclass node for ghost
      SubClassNode.NodeValue := LowerCase(RouteSubClass + Copy(SubClassNode.NodeValue, 5));
    end
    else
    begin
      // Subclass node for via/shaping
      if (SubClassNode.NodeValue <> DirectRoutingClass) then
      begin
        SubClassNode.NodeValue := LowerCase(DirectRoutingClass);
        result := true;
      end;
    end;
  end;
end;

function ProcessGpxRpt(const GpxRptNode: TXmlNode): boolean;
begin
  result := ProcessSubClass(GpxRptNode);
end;

function ProcessRtePt(const RtePtNode: TXmlNode): boolean;
var ExtensionNode: TXmlNode;
    RtePtExtensions: TXmlNode;
    GpxRptNode: TXmlNode;
    HasGpxRpt: boolean;
begin
  result := false;
  RtePtExtensions := RtePtNode.Find(ExtensionsNodename);
  ProcessCoords(RtePtNode);
  if (RtePtExtensions = nil) then
    exit;
  ExtensionNode := RtePtExtensions.Find(RoutePointExtensionsNodename);
  result := ProcessSubClass(ExtensionNode);
  for GpxRptNode in ExtensionNode.ChildNodes do
  begin
    ProcessCoords(GpxRptNode);

    HasGpxRpt := ProcessGpxRpt(GpxRptNode);
    result := result or HasGpxRpt;
  end;
end;

function ProcessRte(const RteNode: TXmlNode): integer;
var RtePtNode: TXmlNode;
    RtePts: TXmlNodeList;
begin
  result := 0;

  RtePts := RteNode.FindNodes(RtePtNodename);
  try
    if (RtePts <> nil) then
    begin
      for RtePtNode in RtePts do
      begin
        if (ProcessRtePt(RtePtNode)) then
        begin
          Writeln(Format('Subclass cleared for RoutePoint %s',
                         [FindSubNodeValue(RtePtNode, RtePtNameNodeName)]));
          Inc(result);
        end;
      end;
    end;
  finally
    RtePts.Free;
  end;
end;

function ProcessGpx(const GpxNode: TXmlNode): boolean;
var RteNode: TXmlNode;
    RouteName: string;
    Cnt: integer;
begin
  result := false;

  for RteNode in GpxNode.ChildNodes do
  begin
    if (RteNode.Name = RteNodename) then // Only want <rte> nodes. No <trk> or <wpt>
    begin
      RouteName := FindSubNodeValue(RteNode, RteNameNodeName);
      Writeln(Format('Checking route %s', [RouteName]));

      Cnt := ProcessRte(RteNode);
      result := result or (Cnt > 0);

      Writeln(Format('SubClass cleared for %d RoutePoints in %s', [Cnt, RouteName]));
      Writeln;
    end;
  end;
end;

begin
  if (ParamCount < 1) then
  begin
    Writeln(Format('Usage %s <path to GPX file>', [ParamStr(0)]));
    halt(1);
  end;

  GpxFile := ParamStr(1);

  if not (FileExists(GpxFile)) then
  begin
    Writeln(Format('Gpx file %s not found.', [GpxFile]));
    halt(2);
  end;

  FormatSettings := GetLocaleSetting;

  Xml := TXmlVerySimple.Create;
  try
    try
      Xml.LoadFromFile(GpxFile);
    except
      Writeln('Could not load GPX as XML');
      halt(3);
    end;

    GpxNode := Xml.ChildNodes.find(GpxNodename);  // Look for <gpx> node
    if (GpxNode = nil) or
       (GpxNode.Name <> GpxNodename) then
    begin
      Writeln(Format('No %s node found in %s', [GpxNodename, GpxFile] ));
      halt(4);
    end;

    ProcessGPX(GpxNode);           // There can be only 1 <gpx> node
    Xml.SaveToFile(GpxFile);

    Writeln(Format('Gpx file %s saved', [GpxFile]));
  finally
    Xml.Free;
  end;

end.
